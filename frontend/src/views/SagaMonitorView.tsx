import React, { useEffect, useState } from 'react';
import { ApiClient } from '../api';
import { SagaInstance } from '../types';
import { RefreshCw, CheckCircle2, XCircle, Clock, RotateCcw, GitBranch, ArrowRight } from 'lucide-react';

export const SagaMonitorView: React.FC = () => {
  const [sagas, setSagas] = useState<SagaInstance[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedSaga, setSelectedSaga] = useState<SagaInstance | null>(null);

  const fetchSagas = async () => {
    try {
      setLoading(true);
      const data = await ApiClient.request<SagaInstance[]>('/operations/sagas');
      setSagas(data);
      if (data.length > 0 && !selectedSaga) {
        setSelectedSaga(data[0]);
      }
    } catch (err: any) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSagas();
  }, []);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-800 pb-5">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Distributed Saga Orchestration Monitor</h1>
          <p className="text-sm text-slate-400 mt-1">Multi-step financial transfer state machine, compensating transactions & recovery tracking.</p>
        </div>
        <button
          onClick={fetchSagas}
          className="inline-flex items-center gap-2 px-3.5 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 text-xs font-medium transition"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          Refresh Workflows
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left: Saga Instances List */}
        <div className="lg:col-span-1 rounded-xl bg-slate-900 border border-slate-800 p-4 space-y-3">
          <div className="text-xs font-semibold text-slate-400 uppercase tracking-wider flex items-center justify-between">
            <span>Saga Executions ({sagas.length})</span>
            <GitBranch className="w-4 h-4 text-indigo-400" />
          </div>

          <div className="space-y-2 max-h-[65vh] overflow-y-auto pr-1">
            {sagas.length === 0 ? (
              <p className="text-xs text-slate-500 py-6 text-center">No active or recorded sagas yet.</p>
            ) : (
              sagas.map((s) => (
                <div
                  key={s.sagaId}
                  onClick={() => setSelectedSaga(s)}
                  className={`p-3 rounded-lg border cursor-pointer transition text-xs ${
                    selectedSaga?.sagaId === s.sagaId
                      ? 'bg-slate-800 border-indigo-500/80 shadow'
                      : 'bg-slate-950/60 border-slate-800/80 hover:bg-slate-800/50'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-white">{s.sagaType}</span>
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-medium ${
                        s.status === 'COMPLETED'
                          ? 'bg-emerald-950/60 text-emerald-400 border border-emerald-800'
                          : s.status === 'FAILED'
                          ? 'bg-rose-950/60 text-rose-400 border border-rose-800'
                          : 'bg-purple-950/60 text-purple-400 border border-purple-800'
                      }`}
                    >
                      {s.status}
                    </span>
                  </div>
                  <div className="text-[11px] font-mono text-slate-400 mt-1 truncate">ID: {s.sagaId}</div>
                  <div className="text-[10px] text-slate-500 mt-2">{new Date(s.createdAt).toLocaleTimeString()}</div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Right: Selected Saga Detailed Execution Steps */}
        <div className="lg:col-span-2 rounded-xl bg-slate-900 border border-slate-800 p-6 flex flex-col justify-between">
          {selectedSaga ? (
            <div className="space-y-6">
              <div>
                <div className="flex items-center justify-between">
                  <h2 className="text-base font-bold text-white flex items-center gap-2">
                    <span>Saga Workflow: {selectedSaga.sagaType}</span>
                  </h2>
                  <span
                    className={`px-2.5 py-1 rounded text-xs font-semibold ${
                      selectedSaga.status === 'COMPLETED'
                        ? 'bg-emerald-950 text-emerald-400 border border-emerald-800'
                        : selectedSaga.status === 'FAILED'
                        ? 'bg-rose-950 text-rose-400 border border-rose-800'
                        : 'bg-amber-950 text-amber-400 border border-amber-800'
                    }`}
                  >
                    Status: {selectedSaga.status}
                  </span>
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 mt-4 text-xs font-mono bg-slate-950 p-3 rounded-lg border border-slate-800">
                  <div>
                    <span className="text-slate-500 block text-[10px] font-sans">CORRELATION ID</span>
                    <span className="text-white truncate block">{selectedSaga.correlationId}</span>
                  </div>
                  <div>
                    <span className="text-slate-500 block text-[10px] font-sans">CURRENT STATE</span>
                    <span className="text-indigo-400 block">{selectedSaga.currentState}</span>
                  </div>
                  <div>
                    <span className="text-slate-500 block text-[10px] font-sans">RETRY COUNT</span>
                    <span className="text-white block">{selectedSaga.retryCount}</span>
                  </div>
                </div>
              </div>

              {/* Execution Steps Timeline */}
              <div className="space-y-4">
                <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Sequential Execution Pipeline</h3>
                <div className="space-y-3">
                  {selectedSaga.steps.map((st, idx) => (
                    <div
                      key={st.stepId}
                      className="p-3.5 rounded-lg bg-slate-950 border border-slate-800 flex items-center justify-between gap-4"
                    >
                      <div className="flex items-center gap-3">
                        <div
                          className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${
                            st.status === 'COMPLETED'
                              ? 'bg-emerald-900/60 text-emerald-400 border border-emerald-700'
                              : st.status === 'FAILED'
                              ? 'bg-rose-900/60 text-rose-400 border border-rose-700'
                              : 'bg-indigo-900/60 text-indigo-400 border border-indigo-700'
                          }`}
                        >
                          {st.status === 'COMPLETED' ? <CheckCircle2 className="w-3.5 h-3.5" /> : idx + 1}
                        </div>
                        <div>
                          <div className="text-xs font-semibold text-white">{st.stepName}</div>
                          {st.errorMessage && (
                            <div className="text-[11px] text-rose-400 mt-0.5">{st.errorMessage}</div>
                          )}
                        </div>
                      </div>

                      <div className="text-right text-[11px] font-mono">
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-sans font-medium ${
                            st.status === 'COMPLETED' ? 'text-emerald-400' : 'text-rose-400'
                          }`}
                        >
                          {st.status}
                        </span>
                        {st.completedAt && (
                          <div className="text-slate-500 text-[10px] mt-0.5">
                            {new Date(st.completedAt).toLocaleTimeString()}
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            <p className="text-xs text-slate-500 py-16 text-center">Select a saga execution on the left to inspect its multi-step pipeline.</p>
          )}

          <div className="mt-6 pt-4 border-t border-slate-800 text-[11px] text-slate-500">
            Sagas maintain state persistence in PostgreSQL to survive service crash/restart and execute compensating balance releases.
          </div>
        </div>
      </div>
    </div>
  );
};
