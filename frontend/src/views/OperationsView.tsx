import React, { useEffect, useState } from 'react';
import { ApiClient } from '../api';
import { Server, Database, Cpu, Activity, RefreshCw, Zap, Play } from 'lucide-react';

export const OperationsView: React.FC = () => {
  const [health, setHealth] = useState<any>(null);
  const [summary, setSummary] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  // Generator states
  const [accountCount, setAccountCount] = useState(50);
  const [transactionCount, setTransactionCount] = useState(200);
  const [seed, setSeed] = useState(42);
  const [generating, setGenerating] = useState(false);
  const [genResult, setGenResult] = useState<any>(null);

  const fetchOpsData = async () => {
    try {
      setLoading(true);
      const [h, s] = await Promise.all([
        ApiClient.request<any>('/operations/health'),
        ApiClient.request<any>('/operations/summary')
      ]);
      setHealth(h);
      setSummary(s);
    } catch (err: any) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOpsData();
  }, []);

  const handleGenerateData = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setGenerating(true);
      setGenResult(null);
      const res = await ApiClient.request<any>(
        `/operations/synthetic-data/generate?accounts=${accountCount}&transactions=${transactionCount}&seed=${seed}`,
        { method: 'POST' }
      );
      setGenResult(res);
      await fetchOpsData();
    } catch (err: any) {
      alert(`Generator failed: ${err.message}`);
    } finally {
      setGenerating(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-800 pb-5">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">System Operations & Diagnostic Console</h1>
          <p className="text-sm text-slate-400 mt-1">Platform telemetry, database connection pools, memory consumption & synthetic benchmark generator.</p>
        </div>
        <button
          onClick={fetchOpsData}
          className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {/* Health Metrics */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-between">
          <div>
            <span className="text-xs text-slate-400 uppercase font-semibold">Service Health</span>
            <div className="text-lg font-bold text-emerald-400 mt-1 flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse" />
              {health?.status || 'UP'}
            </div>
          </div>
          <Server className="w-6 h-6 text-slate-600" />
        </div>

        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-between">
          <div>
            <span className="text-xs text-slate-400 uppercase font-semibold">PostgreSQL Driver</span>
            <div className="text-lg font-bold text-white mt-1">
              {health?.database || 'CONNECTED'}
            </div>
          </div>
          <Database className="w-6 h-6 text-slate-600" />
        </div>

        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-between">
          <div>
            <span className="text-xs text-slate-400 uppercase font-semibold">JVM Uptime</span>
            <div className="text-lg font-bold text-white font-mono mt-1">
              {health ? Math.round(health.jvmUptimeMs / 1000) : 0}s
            </div>
          </div>
          <Activity className="w-6 h-6 text-slate-600" />
        </div>

        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-between">
          <div>
            <span className="text-xs text-slate-400 uppercase font-semibold">CPU Hardware Cores</span>
            <div className="text-lg font-bold text-indigo-400 font-mono mt-1">
              {health?.availableProcessors || 4} Threads
            </div>
          </div>
          <Cpu className="w-6 h-6 text-slate-600" />
        </div>
      </div>

      {/* Synthetic Benchmark Data Generator Panel */}
      <div className="rounded-xl bg-slate-900 border border-slate-800 p-6 space-y-5">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div>
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <Zap className="w-4 h-4 text-amber-400" />
              Deterministic Synthetic Data Generator
            </h2>
            <p className="text-xs text-slate-400 mt-1">
              Simulate realistic enterprise accounting workloads, load stress bursts, and idempotency tests using deterministic pseudo-random seeds.
            </p>
          </div>
        </div>

        {genResult && (
          <div className="p-4 rounded-lg bg-emerald-950/50 border border-emerald-800 text-emerald-300 text-xs font-mono">
            Successfully generated {genResult.accountsGenerated} accounts and {genResult.transactionsGenerated} transactions with seed {genResult.seedUsed}.
          </div>
        )}

        <form onSubmit={handleGenerateData} className="grid grid-cols-1 sm:grid-cols-4 gap-4 text-xs font-sans">
          <div>
            <label className="block text-slate-300 font-medium mb-1">Accounts to Generate</label>
            <input
              type="number"
              min="5"
              max="1000"
              value={accountCount}
              onChange={(e) => setAccountCount(parseInt(e.target.value))}
              className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-white font-mono focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div>
            <label className="block text-slate-300 font-medium mb-1">Transactions to Generate</label>
            <input
              type="number"
              min="10"
              max="5000"
              value={transactionCount}
              onChange={(e) => setTransactionCount(parseInt(e.target.value))}
              className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-white font-mono focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div>
            <label className="block text-slate-300 font-medium mb-1">Deterministic Random Seed</label>
            <input
              type="number"
              value={seed}
              onChange={(e) => setSeed(parseInt(e.target.value))}
              className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-white font-mono focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div className="flex items-end">
            <button
              type="submit"
              disabled={generating}
              className="w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg bg-amber-600 hover:bg-amber-500 text-white font-semibold transition disabled:opacity-50"
            >
              <Play className="w-4 h-4" />
              {generating ? 'Generating Data...' : 'Run Generation'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
