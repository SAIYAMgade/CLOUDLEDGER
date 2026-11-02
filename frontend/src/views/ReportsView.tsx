import React, { useEffect, useState } from 'react';
import { ApiClient } from '../api';
import { Download, RefreshCw, BarChart3, FileSpreadsheet } from 'lucide-react';

export const ReportsView: React.FC = () => {
  const [dailySummary, setDailySummary] = useState<any>(null);
  const [volumeByType, setVolumeByType] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(true);

  const fetchReports = async () => {
    try {
      setLoading(true);
      const [sum, vol] = await Promise.all([
        ApiClient.request<any>('/reports/daily-transactions'),
        ApiClient.request<Record<string, number>>('/reports/transaction-volume')
      ]);
      setDailySummary(sum);
      setVolumeByType(vol);
    } catch (err: any) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReports();
  }, []);

  const handleDownloadCsv = () => {
    window.open('/api/v1/reports/exports', '_blank');
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-800 pb-5">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Financial & Regulatory Reports</h1>
          <p className="text-sm text-slate-400 mt-1">Authoritative daily summaries, transaction volumes, and audit-grade CSV data exports.</p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={fetchReports}
            className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <button
            onClick={handleDownloadCsv}
            className="inline-flex items-center gap-2 px-3.5 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs transition"
          >
            <Download className="w-4 h-4" />
            Export Full Ledger CSV
          </button>
        </div>
      </div>

      {/* Daily Summary Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800">
          <span className="text-xs uppercase tracking-wider text-slate-400 font-semibold block">Total Processed Volume</span>
          <div className="text-2xl font-bold text-white font-mono mt-3">
            ${dailySummary?.totalProcessedValue?.toLocaleString() || '0.00'}
          </div>
          <div className="text-xs text-slate-400 mt-2">
            Based on all authoritative posted double-entry journal entries.
          </div>
        </div>

        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800">
          <span className="text-xs uppercase tracking-wider text-slate-400 font-semibold block">Transaction Success Rate</span>
          <div className="text-2xl font-bold text-emerald-400 font-mono mt-3">
            {dailySummary?.successRatePercentage ?? '100'}%
          </div>
          <div className="text-xs text-slate-400 mt-2">
            {dailySummary?.successfulTransactions || 0} succeeded out of {dailySummary?.totalTransactions || 0} total.
          </div>
        </div>

        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800">
          <span className="text-xs uppercase tracking-wider text-slate-400 font-semibold block">Reversals & Compensations</span>
          <div className="text-2xl font-bold text-purple-400 font-mono mt-3">
            {dailySummary?.reversedTransactions || 0}
          </div>
          <div className="text-xs text-slate-400 mt-2">
            Corrected using auditable double-entry reversal entries.
          </div>
        </div>
      </div>

      {/* Transaction Volumes by Type */}
      <div className="rounded-xl bg-slate-900 border border-slate-800 p-6 space-y-4">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <h2 className="text-sm font-bold text-white flex items-center gap-2">
            <BarChart3 className="w-4 h-4 text-indigo-400" />
            Volume Distribution by Transaction Type
          </h2>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-5 gap-4">
          {Object.entries(volumeByType).map(([type, count]) => (
            <div key={type} className="p-4 rounded-lg bg-slate-950 border border-slate-800">
              <span className="text-[11px] text-slate-400 font-medium block">{type}</span>
              <span className="text-xl font-bold font-mono text-white mt-1 block">{count}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
