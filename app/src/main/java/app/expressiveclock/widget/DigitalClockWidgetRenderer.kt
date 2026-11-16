package app.expressiveclock.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import app.expressiveclock.MainActivity
import app.expressiveclock.R
import app.expressiveclock.data.ClockSettings
import app.expressiveclock.data.ClockStore
import app.expressiveclock.data.DigitalBackground
import app.expressiveclock.data.DigitalLayout
import app.expressiveclock.data.DigitalTypefaceWeight
import app.expressiveclock.data.WidgetConfig
import app.expressiveclock.data.WidgetFontFamily
import app.expressiveclock.data.WidgetType
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.roundToInt

object DigitalClockWidgetRenderer {

    suspend fun update(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
        val store = ClockStore(context)
        val config = store.widgetConfigNow(appWidgetId) ?: WidgetConfig(type = WidgetType.DIGITAL)
        if (config.type == WidgetType.ANALOG) {
            ClockWidgetRenderer.update(context, manager, appWidgetId)
            return
        }
        val settings = store.settingsNow()
        val options = manager.getAppWidgetOptions(appWidgetId)

        val views = try {
            build(context, config, settings, options)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // Font/artwork failures must not leave a new widget blank or displaying stale time.
            android.util.Log.e("DigitalClockWidget", "Rendering native clock fallback", error)
            RemoteViews(context.packageName, R.layout.widget_digital_initial).apply {
                setOnClickPendingIntent(android.R.id.background, PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }
        }
        manager.updateAppWidget(appWidgetId, views)
    }

    private suspend fun build(
        context: Context,
        config: WidgetConfig,
        settings: ClockSettings,
        options: Bundle,
    ): RemoteViews {
        val density = context.resources.displayMetrics.density
        fun px(dp: Int) = (dp * density).roundToInt().coerceAtLeast(1)

        val sizes: List<SizeF> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            @Suppress("DEPRECATION")
            options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
                ?.takeIf { it.isNotEmpty() }
                ?: emptyList()
        } else {
            emptyList()
        }

        if (sizes.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val map = sizes.associateWith { size ->
                one(context, config, settings, px(size.width.roundToInt()), px(size.height.roundToInt()))
            }
            return RemoteViews(map)
        }

        val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
        val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minW)
        val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 70)
        val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minH)
        val portrait = one(context, config, settings, px(minW), px(maxH))
        val landscape = one(context, config, settings, px(maxW), px(minH))
        return RemoteViews(landscape, portrait)
    }

    private suspend fun one(
        context: Context,
        config: WidgetConfig,
        settings: ClockSettings,
        w: Int,
        h: Int,
    ): RemoteViews {
        val colours = WidgetColours.resolve(context, config, settings)
        val profile = NativeWidgetFonts.resolve(context, config.digitalFont, config.digitalWeight)
        val layout = profile.layout(config.digitalLayout == DigitalLayout.STACKED)
        val views = RemoteViews(context.packageName, layout)

        // These are the same restricted-context native paints used by the device preview.
        val (timePaint, datePaint) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
            profile.paints(context, layout)
        }
        val bmp = renderDigitalBitmap(context, config, colours, w, h, views, timePaint, datePaint, profile)
        views.setImageViewIcon(R.id.background_shape, bmp.toIcon())

        views.setOnClickPendingIntent(
            android.R.id.background,
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )

        return views
    }

    private fun renderDigitalBitmap(
        context: Context,
        config: WidgetConfig,
        colours: FaceRenderer.Colours,
        w: Int,
        h: Int,
        views: RemoteViews,
        timePaint: Paint,
        datePaint: Paint,
        profile: NativeWidgetFonts.Profile,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // 1. Draw Background Shape
        if (config.digitalBackground != DigitalBackground.NONE) {
            val rx = (w.coerceAtMost(h) * 0.22f)
            val rect = RectF(4f, 4f, w - 4f, h - 4f)
            if (config.digitalBackground == DigitalBackground.FILLED) {
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colours.face
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(rect, rx, rx, fillPaint)
            } else if (config.digitalBackground == DigitalBackground.OUTLINED) {
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colours.outline
                    style = Paint.Style.STROKE
                    strokeWidth = (w.coerceAtMost(h) * 0.03f).coerceAtLeast(3f)
                }
                canvas.drawRoundRect(rect, rx, rx, strokePaint)
            }
        }

        val isStacked = config.digitalLayout == DigitalLayout.STACKED
        val isTimeOnly = !config.showDate && !config.showDayOfWeek
        val isCompact = config.digitalLayout == DigitalLayout.COMPACT

        val dateSkeleton = when {
            isTimeOnly || (!config.showDate && !config.showDayOfWeek) -> ""
            config.showDate && config.showDayOfWeek -> "EEEdMMM"
            config.showDayOfWeek -> "EEE"
            else -> "dMMM"
        }
        val showDateView = dateSkeleton.isNotEmpty()

        val hourColorInt = config.digitalHourColor?.toInt() ?: colours.hourHand
        val colonColorInt = config.digitalColonColor?.toInt() ?: colours.onFace
        val minuteColorInt = config.digitalMinuteColor?.toInt() ?: colours.minuteHand
        val dateColorInt = config.digitalDateColor?.toInt() ?: colours.minor

        fun clock(id: Int, twelve: String, twentyFour: String) {
            views.setCharSequence(id, "setFormat12Hour", twelve)
            views.setCharSequence(id, "setFormat24Hour", twentyFour)
        }
        clock(R.id.live_hours, "hh", "HH")
        val minutes = if (config.showSeconds && !isCompact) "mm:ss" else "mm"
        clock(R.id.live_minutes, minutes, minutes)
        val datePattern = if (showDateView)
            DateFormat.getBestDateTimePattern(Locale.getDefault(), dateSkeleton) else ""
        val styledDate = android.text.SpannableString(datePattern)
        if (config.showDayOfWeek) {
            val start = datePattern.indexOf('E')
            if (start >= 0) {
                val end = (start until datePattern.length).firstOrNull { datePattern[it] != 'E' } ?: datePattern.length
                styledDate.setSpan(android.text.style.ForegroundColorSpan(
                    config.digitalDayColor?.toInt() ?: colours.minor), start, end,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        views.setCharSequence(R.id.live_date, "setFormat12Hour", styledDate)
        views.setCharSequence(R.id.live_date, "setFormat24Hour", styledDate)
        views.setBoolean(R.id.live_date, "setAllCaps", true)
        views.setViewVisibility(R.id.live_date, if (showDateView) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.live_row, if (isStacked) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.live_hours, View.VISIBLE)
        views.setViewVisibility(R.id.live_minutes, View.VISIBLE)

        // Move the same clock views between horizontal and stacked arrangements by selecting
        // the matching layout; all positions use the original bitmap's size/baseline formulas.
        var size = if (isStacked) h * (if (showDateView) .30f else .38f)
            else h * (if (isCompact) .22f else .38f)
        var dateSize = size * (if (isStacked) .28f else if (isCompact) .45f else .32f)
        timePaint.textSize = size
        // Reserve both advance width and ink overhang. A horizontal LinearLayout otherwise
        // measures its final clock against only the remaining width, clipping the last digit.
        fun inkWidth(text: String, paint: Paint): Float {
            val bounds = android.graphics.Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            return maxOf(paint.measureText(text), bounds.right.toFloat()) -
                minOf(0f, bounds.left.toFloat())
        }
        fun pairWidth(paint: Paint) = (0..59).maxOf {
            inkWidth(String.format(Locale.getDefault(), "%02d", it), paint)
        }
        val gutter = 2f * context.resources.displayMetrics.density
        val pair = pairWidth(timePaint)
        val colon = inkWidth(":", timePaint)
        val minute = if (config.showSeconds && !isCompact) pair * 2f + colon else pair
        val scale = DigitalTextFit.scale(w.toFloat(), pair, colon, minute, isStacked, gutter)
        size *= scale
        dateSize *= scale
        timePaint.textSize = size
        datePaint.textSize = dateSize
        // The fit uses the widest possible time, while native views keep natural advances.
        // Tiny symmetric padding protects italic/curved glyph overhangs without fixed digit slots.
        listOf(R.id.live_hours, R.id.live_minutes, R.id.live_colon, R.id.live_date).forEach {
            views.setViewPadding(it, gutter.roundToInt() / 2, 0, gutter.roundToInt() / 2, 0)
        }
        if (showDateView) {
            val formatter = java.text.SimpleDateFormat(datePattern, Locale.getDefault())
            val calendar = java.util.Calendar.getInstance().apply { set(2024, 0, 1, 12, 0, 0) }
            val widest = (0 until 366).maxOf {
                val value = formatter.format(calendar.time).uppercase(Locale.getDefault())
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                inkWidth(value, datePaint)
            }
            dateSize *= ((w - 2 * gutter).coerceAtLeast(1f) / widest.coerceAtLeast(1f)).coerceAtMost(1f)
        }
        fun style(id: Int, sizePx: Float, color: Int) {
            views.setTextViewTextSize(id, android.util.TypedValue.COMPLEX_UNIT_PX, sizePx)
            views.setTextColor(id, color)
            profile.variation?.let { views.setString(id, "setFontVariationSettings", it) }
        }
        style(R.id.live_hours, size, hourColorInt)
        style(R.id.live_minutes, size, minuteColorInt)
        style(R.id.live_colon, size, colonColorInt)
        style(R.id.live_date, dateSize, dateColorInt)
        timePaint.textSize = size
        datePaint.textSize = dateSize
        fun top(slot: Int, baseline: Float, paint: Paint) {
            views.setViewPadding(slot, 0, (baseline + paint.fontMetrics.ascent).roundToInt().coerceAtLeast(0), 0, 0)
        }
        val cy = h / 2f
        if (isStacked) {
            top(R.id.live_hour_slot, cy - size * .05f, timePaint)
            top(R.id.live_minute_slot, cy + size * .75f, timePaint)
            top(R.id.live_date_slot, cy + size * 1.18f, datePaint)
        } else {
            val baseline = if (showDateView) cy - size * .10f else cy + size * .32f
            top(R.id.live_row_slot, baseline, timePaint)
            top(R.id.live_date_slot, baseline + size * .48f, datePaint)
        }

        return bmp
    }

    private fun Bitmap.toIcon(): Icon {
        val out = ByteArrayOutputStream(64 * 1024)
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSLESS
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        compress(format, 100, out)
        val bytes = out.toByteArray()
        return Icon.createWithData(bytes, 0, bytes.size)
    }
}
