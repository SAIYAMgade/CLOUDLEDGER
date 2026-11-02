import React, { useEffect, useState } from 'react';
import { ApiClient } from '../api';
import { AuditLog } from '../types';
import { ShieldCheck, ShieldAlert, RefreshCw, Key, Link2, CheckCircle2 } from 'lucide-react';

export const AuditView: React.FC = () => {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [verification, setVerification] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [verifying, setVerifying] = useState(false);

  const fetchAuditData = async () => {
    try {
      setLoading(true);
      const [logsPage, verif] = await Promise.all([
        ApiClient.request<{ content: AuditLog[] }>('/audit-logs?page=0&size=50'),
        ApiClient.request<any>('/audit-logs/verify')
      ]);
      setLogs(logsPage.content || []);
      setVerification(verif);
    } catch (err: any) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAuditData();
  }, []);

  const handleVerify = async () => {
    try {
      setVerifying(true);
      const verif = await ApiClient.request<any>('/audit-logs/verify');
      setVerification(verif);
    } catch (err: any) {
      alert(`Verification failed: ${err.message}`);
    } finally {
      setVerifying(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-800 pb-5">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Tamper-Evident Audit & Compliance Trail</h1>
          <p className="text-sm text-slate-400 mt-1">Cryptographically chained SHA-256 audit ledger verifying immutable operational history.</p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={handleVerify}
            disabled={verifying}
            className="inline-flex items-center gap-2 px-3.5 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white font-medium text-xs transition"
          >
            <ShieldCheck className={`w-4 h-4 ${verifying ? 'animate-spin' : ''}`} />
            Verify Cryptographic Chain
          </button>
          <button
            onClick={fetchAuditData}
            className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Verification Status Card */}
      {verification && (
        <div
          className={`p-5 rounded-xl border flex flex-col sm:flex-row sm:items-center justify-between gap-4 ${
            verification.verified
              ? 'bg-emerald-950/40 border-emerald-800/80'
              : 'bg-rose-950/40 border-rose-800/80'
          }`}
        >
          <div className="flex items-center gap-3.5">
            <div className={`p-2.5 rounded-lg ${verification.verified ? 'bg-emerald-900/60 text-emerald-400' : 'bg-rose-900/60 text-rose-400'}`}>
              {verification.verified ? <ShieldCheck className="w-6 h-6" /> : <ShieldAlert className="w-6 h-6" />}
            </div>
            <div>
              <div className="text-sm font-bold text-white flex items-center gap-2">
                <span>{verification.verified ? 'Cryptographic Hash-Chain Intact' : 'CRITICAL: Hash-Chain Tamper Detected!'}</span>
                <span className="px-2 py-0.5 rounded text-[10px] bg-emerald-900/80 text-emerald-300 font-mono">
                  {verification.recordCount} Verified Blocks
                </span>
              </div>
              <div className="text-xs text-slate-300 mt-1">
                {verification.message || 'Every audit block verifies SHA256(prev_hash + user + action + payload).'}
              </div>
            </div>
          </div>
          {verification.latestHash && (
            <div className="text-right font-mono text-[11px] text-slate-400 bg-slate-950/80 p-2.5 rounded-lg border border-slate-800 max-w-sm truncate">
              <span className="text-[10px] text-slate-500 block font-sans">LATEST MERKLE TIP HASH</span>
              <span className="text-emerald-400">{verification.latestHash}</span>
            </div>
          )}
        </div>
      )}

      {/* Audit Log Table */}
      <div className="rounded-xl bg-slate-900 border border-slate-800 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs font-mono">
            <thead className="bg-slate-950 text-slate-400 font-semibold border-b border-slate-800 font-sans">
              <tr>
                <th className="py-3 px-4">Block #</th>
                <th className="py-3 px-4">Action</th>
                <th className="py-3 px-4">User</th>
                <th className="py-3 px-4">Entity Type / ID</th>
                <th className="py-3 px-4">SHA-256 Current Hash</th>
                <th className="py-3 px-4">Timestamp</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {logs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-slate-500 font-sans">
                    No audit records logged yet.
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr key={log.auditId} className="hover:bg-slate-800/40 transition">
                    <td className="py-3 px-4 text-indigo-400 font-bold">#{log.auditId}</td>
                    <td className="py-3 px-4 font-sans">
                      <span className="px-2 py-0.5 rounded text-[10px] font-medium bg-slate-800 text-slate-200 border border-slate-700">
                        {log.action}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-white font-sans">{log.userId}</td>
                    <td className="py-3 px-4 text-slate-300">
                      {log.entityType}: <span className="text-slate-400">{log.entityId}</span>
                    </td>
                    <td className="py-3 px-4 text-slate-400 truncate max-w-[140px] text-[11px]" title={log.currHash}>
                      {log.currHash}
                    </td>
                    <td className="py-3 px-4 text-slate-400 font-sans">
                      {new Date(log.createdAt).toLocaleString()}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
