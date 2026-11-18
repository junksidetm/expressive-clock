import base64
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock, patch
import delivery_preflight as policy

class PreflightTest(unittest.TestCase):
    def replies(self, content=b'reviewed'):
        return [dict(private=True,owner={'login':'samuelbeckman'},default_branch='main'),
            [{'login':'samuelbeckman'}],dict(total_count=1,workflows=[dict(state='active',path=policy.WORKFLOW)]),
            {'commit':{'sha':'1'*40}},dict(encoding='base64',content=base64.b64encode(content).decode())]+[{'workflow_runs':[]} for _ in range(5)]
    def inspect(self,replies):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/policy.WORKFLOW;p.parent.mkdir(parents=True);p.write_bytes(b'reviewed')
            return policy.inspect(Mock(side_effect=replies),Path(d))
    def test_exact_owner_only_reviewed_workflow(self):
        self.assertEqual('1'*40,self.inspect(self.replies())[1])
    def test_rejects_public_repo(self):
        r=self.replies();r[0]['private']=False
        with self.assertRaises(ValueError):self.inspect(r)
    def test_public_repo_requires_explicit_approval(self):
        r=self.replies();r[0]['private']=False
        with tempfile.TemporaryDirectory() as d, patch.object(policy.Path,'home',return_value=Path(d)):
            with self.assertRaises(ValueError):self.inspect(r)
            config=Path(d)/'.local/state/expressive-clock/delivery-config.json'
            config.parent.mkdir(parents=True);config.write_text('{"allow_public_artifacts":true}')
            r=self.replies();r[0]['private']=False
            self.assertEqual('1'*40,self.inspect(r)[1])
    def test_rejects_other_collaborators_without_changing_permissions(self):
        r=self.replies();r[1].append({'login':'someone-else'})
        with self.assertRaises(ValueError):self.inspect(r)
    def test_rejects_other_active_workflow(self):
        r=self.replies();r[2]['workflows'].append(dict(state='active',path='.github/workflows/other.yml'))
        with self.assertRaises(ValueError):self.inspect(r)
    def test_rejects_changed_remote_workflow(self):
        with self.assertRaises(ValueError):self.inspect(self.replies(b'changed'))
    def test_rejects_unrelated_queued_run(self):
        r=self.replies();r[5]={'workflow_runs':[{'id':88}]}
        with self.assertRaises(ValueError):self.inspect(r)
    def test_checks_old_waiting_runs(self):
        r=self.replies();r[7]={'workflow_runs':[{'id':88}]}
        with self.assertRaises(ValueError):self.inspect(r)
    def test_only_own_dispatch_allowed(self):
        policy.no_other_runs(Mock(return_value={'workflow_runs':[{'id':5}]}),5)
        with self.assertRaises(ValueError):policy.no_other_runs(Mock(return_value={'workflow_runs':[{'id':6}]}),5)
    def test_rejects_wrong_commit_or_event(self):
        run=dict(event='workflow_dispatch',path=policy.WORKFLOW,head_branch='main',head_sha='1'*40,
            head_repository={'full_name':policy.REPO},display_title='Local APK '+'a'*32)
        policy.check_run(run,{'default_branch':'main'},'1'*40,'a'*32)
        for change in [{'head_sha':'2'*40},{'event':'pull_request'},{'head_branch':'feature'},{'path':'.github/workflows/other.yml'}]:
            with self.assertRaises(ValueError):policy.check_run(run|change,{'default_branch':'main'},'1'*40,'a'*32)
