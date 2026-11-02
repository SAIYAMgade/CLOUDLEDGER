import React, { useState } from 'react';
import { ApiClient } from '../api';
import { Lock, Shield, User, ArrowRight } from 'lucide-react';

interface LoginViewProps {
  onLoginSuccess: (user: any) => void;
}

export const LoginView: React.FC<LoginViewProps> = ({ onLoginSuccess }) => {
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('password123');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setLoading(true);
      setErrorMsg('');
      const resp = await ApiClient.request<any>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password })
      });
      ApiClient.setToken(resp.accessToken);
      onLoginSuccess(resp);
    } catch (err: any) {
      setErrorMsg(err.message || 'Authentication failed. Please verify credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickSwitch = (u: string) => {
    setUsername(u);
    setPassword('password123');
  };

  return (
    <div className="min-h-screen bg-slate-950 flex items-center justify-center p-4">
      <div className="max-w-md w-full space-y-6 bg-slate-900 border border-slate-800 p-8 rounded-2xl shadow-2xl">
        <div className="text-center space-y-2">
          <div className="inline-flex p-3 rounded-xl bg-indigo-600/10 border border-indigo-500/20 text-indigo-400 mb-2">
            <Shield className="w-8 h-8" />
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">CloudLedger Portal</h1>
          <p className="text-xs text-slate-400">
            Enterprise Financial Accounting & Distributed Transaction Platform
          </p>
        </div>

        {errorMsg && (
          <div className="p-3 rounded-lg bg-rose-950/60 border border-rose-800 text-rose-300 text-xs">
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleLogin} className="space-y-4 text-xs font-sans">
          <div>
            <label className="block text-slate-300 font-medium mb-1.5">User Identity / Identifier</label>
            <div className="relative">
              <User className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
              <input
                type="text"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full pl-9 pr-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-white focus:outline-none focus:border-indigo-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-slate-300 font-medium mb-1.5">Security Password</label>
            <div className="relative">
              <Lock className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full pl-9 pr-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-white focus:outline-none focus:border-indigo-500"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs transition flex items-center justify-center gap-2 shadow"
          >
            {loading ? 'Authenticating...' : 'Sign In to Ledger Portal'}
            <ArrowRight className="w-4 h-4" />
          </button>
        </form>

        {/* Quick-switch demo accounts */}
        <div className="pt-4 border-t border-slate-800 space-y-2">
          <span className="text-[11px] text-slate-500 font-medium uppercase tracking-wider block">
            Role Preset Quick Selection:
          </span>
          <div className="grid grid-cols-2 gap-2 text-[11px]">
            <button
              onClick={() => handleQuickSwitch('admin')}
              className="p-2 rounded bg-slate-950 border border-slate-800 hover:border-indigo-500 text-slate-300 text-left"
            >
              <div className="font-semibold text-white">Administrator</div>
              <div className="text-[10px] text-slate-500">admin</div>
            </button>
            <button
              onClick={() => handleQuickSwitch('operator')}
              className="p-2 rounded bg-slate-950 border border-slate-800 hover:border-indigo-500 text-slate-300 text-left"
            >
              <div className="font-semibold text-white">Finance Operator</div>
              <div className="text-[10px] text-slate-500">operator</div>
            </button>
            <button
              onClick={() => handleQuickSwitch('approver')}
              className="p-2 rounded bg-slate-950 border border-slate-800 hover:border-indigo-500 text-slate-300 text-left"
            >
              <div className="font-semibold text-white">Finance Approver</div>
              <div className="text-[10px] text-slate-500">approver</div>
            </button>
            <button
              onClick={() => handleQuickSwitch('auditor')}
              className="p-2 rounded bg-slate-950 border border-slate-800 hover:border-indigo-500 text-slate-300 text-left"
            >
              <div className="font-semibold text-white">Auditor (Read-Only)</div>
              <div className="text-[10px] text-slate-500">auditor</div>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
