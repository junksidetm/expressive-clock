#!/usr/bin/env python3
"""Reproduce the original, synthesized alarm fallback. No third-party recording used."""
import math,struct,wave
from pathlib import Path
path=Path(__file__).resolve().parents[1]/'app/src/main/res/raw/alarm_fallback.wav'
with wave.open(str(path),'wb') as output:
    output.setparams((1,2,22050,0,'NONE','not compressed'))
    for i in range(44100):
        t=i/22050
        phase=t%1
        envelope=min(1,phase/.015,max(0,(.65-phase)/.03)) if phase<.65 else 0
        output.writeframesraw(struct.pack('<h',int(18000*envelope*(math.sin(2*math.pi*880*t)+.25*math.sin(2*math.pi*1320*t)))))
print(path)
