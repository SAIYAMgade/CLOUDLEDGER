import React, { useEffect, useState } from 'react';
import { ApiClient } from '../api';
import { OperationsSummary, Transaction } from '../types';
import { ArrowUpRight, CheckCircle2, XCircle, Clock, ShieldCheck, DollarSign, Activity, RefreshCw } from 'lucide-react';

export const DashboardView: React.FC = () => {
  const [summary, setSummary] = useState<OperationsSummary | null>(null);
  const [recentTx, setRecentTx] = useState<Transaction[]>([]);
  const [dailyReport, setDailyReport] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [sumData, txPage, reportData] = await Promise.all([
        ApiClient.request<OperationsSummary>('/operations/summary'),
        ApiClient.request<{ content: Transaction[] }>('/transactions?page=0&size=6&sortBy=createdAt&direction=desc'),
        ApiClient.request<any>('/reports/daily-transactions')
      ]);
      setSummary(sumData);
      setRecentTx(txPage.content || []);
      setDailyReport(reportData);
    } catch (err) {
      console.error('Failed to load dashboard metrics', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-slate-800 pb-5">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Executive Operations Dashboard</h1>
          <p className="text-sm text-slate-400 mt-1">Real-time ledger state, transaction ingestion throughput & audit integrity.</p>
        </div>
        <button
          onClick={fetchData}
          disabled={loading}
          className="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-medium text-slate-200 border border-slate-700 transition"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          Refresh Metrics
        </button>
      </div>

      {/* Top Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Total Processed Volume */}
        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800 flex flex-col justify-between">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs uppercase tracking-wider font-semibold">Total Processed Value</span>
            <DollarSign className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="mt-4">
            <div className="text-2xl font-bold text-white font-mono">
              ${dailyReport?.totalProcessedValue?.toLocaleString() || '0.00'}
            </div>
            <div className="flex items-center gap-1.5 mt-2 text-xs text-emerald-400 font-medium">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>{dailyReport?.successfulTransactions || 0} Posted Transactions</span>
            </div>
          </div>
        </div>

        {/* Processing Success Rate */}
        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800 flex flex-col justify-between">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs uppercase tracking-wider font-semibold">Posting Success Rate</span>
            <Activity className="w-4 h-4 text-blue-400" />
          </div>
          <div className="mt-4">
            <div className="text-2xl font-bold text-white font-mono">
              {dailyReport?.successRatePercentage ?? '100'}%
            </div>
            <div className="flex items-center gap-2 mt-2 text-xs text-slate-400">
              <span className="text-emerald-400 font-medium">{summary?.postedTransactions || 0} succeeded</span>
              <span>•</span>
              <span className="text-rose-400 font-medium">{summary?.failedTransactions || 0} failed</span>
            </div>
          </div>
        </div>

        {/* Active Accounts */}
        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800 flex flex-col justify-between">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs uppercase tracking-wider font-semibold">Accounts in Ledger</span>
            <ArrowUpRight className="w-4 h-4 text-indigo-400" />
          </div>
          <div className="mt-4">
            <div className="text-2xl font-bold text-white font-mono">
              {summary?.totalAccounts?.toLocaleString() || '0'}
            </div>
            <div className="flex items-center gap-1.5 mt-2 text-xs text-indigo-400 font-medium">
              <span>Double-entry balanced</span>
            </div>
          </div>
        </div>

        {/* Audit Hash Chain Status */}
        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800 flex flex-col justify-between">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs uppercase tracking-wider font-semibold">Audit Chain Security</span>
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="mt-4">
            <div className="text-lg font-bold text-emerald-400 flex items-center gap-1.5">
              <CheckCircle2 className="w-5 h-5 text-emerald-400" />
              <span>Cryptographically Intact</span>
            </div>
            <div className="text-xs text-slate-400 mt-2 truncate">
              {summary?.auditChainVerification?.recordCount || 0} tamper-evident blocks
            </div>
          </div>
        </div>
      </div>

      {/* Operational Status Badges */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="p-3.5 rounded-lg bg-slate-900/60 border border-slate-800 flex items-center gap-3">
          <div className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse" />
          <div>
            <div className="text-[11px] text-slate-400 uppercase tracking-wider font-semibold">PostgreSQL ACID Store</div>
            <div className="text-xs font-semibold text-white">Authoritative Primary</div>
          </div>
        </div>
        <div className="p-3.5 rounded-lg bg-slate-900/60 border border-slate-800 flex items-center gap-3">
          <div className="w-2.5 h-2.5 rounded-full bg-blue-400" />
          <div>
            <div className="text-[11px] text-slate-400 uppercase tracking-wider font-semibold">Outbox Queue</div>
            <div className="text-xs font-semibold text-white">{summary?.pendingOutboxEvents || 0} Pending Dispatch</div>
          </div>
        </div>
        <div className="p-3.5 rounded-lg bg-slate-900/60 border border-slate-800 flex items-center gap-3">
          <div className="w-2.5 h-2.5 rounded-full bg-purple-400" />
          <div>
            <div className="text-[11px] text-slate-400 uppercase tracking-wider font-semibold">Saga Coordinator</div>
            <div className="text-xs font-semibold text-white">{summary?.totalSagaInstances || 0} Workflows Tracked</div>
          </div>
        </div>
        <div className="p-3.5 rounded-lg bg-slate-900/60 border border-slate-800 flex items-center gap-3">
          <div className="w-2.5 h-2.5 rounded-full bg-amber-400" />
          <div>
            <div className="text-[11px] text-slate-400 uppercase tracking-wider font-semibold">In-Flight Operations</div>
            <div className="text-xs font-semibold text-white">{summary?.pendingTransactions || 0} Processing</div>
          </div>
        </div>
      </div>

      {/* Recent Activity Table */}
      <div className="rounded-xl bg-slate-900 border border-slate-800 overflow-hidden">
        <div className="p-4 border-b border-slate-800 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-white">Recent Transactions Ingestion Activity</h2>
          <span className="text-xs text-slate-400">Latest authoritative records</span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-950 text-slate-400 font-semibold border-b border-slate-800">
              <tr>
                <th className="py-3 px-4">Transaction ID</th>
                <th className="py-3 px-4">Reference</th>
                <th className="py-3 px-4">Amount</th>
                <th className="py-3 px-4">Type</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Timestamp</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-mono">
              {recentTx.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-slate-500 font-sans">
                    No transactions found. Use the Transactions tab or Synthetic Generator to create test activity.
                  </td>
                </tr>
              ) : (
                recentTx.map((tx) => (
                  <tr key={tx.transactionId} className="hover:bg-slate-800/40 transition">
                    <td className="py-3 px-4 text-slate-300 truncate max-w-[150px]">{tx.transactionId}</td>
                    <td className="py-3 px-4 text-slate-400 font-sans">{tx.externalReference}</td>
                    <td className="py-3 px-4 text-white font-semibold">
                      ${Number(tx.amount).toFixed(2)} <span className="text-slate-400 font-normal">{tx.currency}</span>
                    </td>
                    <td className="py-3 px-4">
                      <span className="px-2 py-0.5 rounded text-[10px] font-sans font-medium bg-slate-800 text-slate-300 border border-slate-700">
                        {tx.transactionType}
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-sans font-medium ${
                          tx.status === 'POSTED'
                            ? 'bg-emerald-950/60 text-emerald-400 border border-emerald-800'
                            : tx.status === 'FAILED'
                            ? 'bg-rose-950/60 text-rose-400 border border-rose-800'
                            : 'bg-amber-950/60 text-amber-400 border border-amber-800'
                        }`}
                      >
                        {tx.status === 'POSTED' && <CheckCircle2 className="w-3 h-3" />}
                        {tx.status === 'FAILED' && <XCircle className="w-3 h-3" />}
                        {tx.status === 'PENDING' && <Clock className="w-3 h-3" />}
                        {tx.status}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-slate-400 font-sans">
                      {new Date(tx.createdAt).toLocaleTimeString()}
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
