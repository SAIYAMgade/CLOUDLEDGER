import React, { useEffect, useState } from 'react';
import { ApiClient } from '../api';
import { Account, Transaction, TransactionEvent, TransactionType } from '../types';
import { Plus, Search, RefreshCw, RotateCcw, Clock, CheckCircle2, XCircle, ArrowRightLeft, Layers } from 'lucide-react';

export const TransactionsView: React.FC = () => {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [showSubmitModal, setShowSubmitModal] = useState(false);
  const [selectedTxEvents, setSelectedTxEvents] = useState<{ tx: Transaction; events: TransactionEvent[] } | null>(null);

  // Form states
  const [sourceAccountId, setSourceAccountId] = useState('');
  const [destAccountId, setDestAccountId] = useState('');
  const [amount, setAmount] = useState('');
  const [currency, setCurrency] = useState('USD');
  const [txType, setTxType] = useState<TransactionType>('TRANSFER');
  const [idempotencyKey, setIdempotencyKey] = useState('');
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [replayNotice, setReplayNotice] = useState<string | null>(null);

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      const [txPage, accList] = await Promise.all([
        ApiClient.request<{ content: Transaction[] }>('/transactions?page=0&size=50&sortBy=createdAt&direction=desc'),
        ApiClient.request<Account[]>('/accounts')
      ]);
      setTransactions(txPage.content || []);
      setAccounts(accList);
      if (accList.length >= 2 && !sourceAccountId) {
        setSourceAccountId(accList[1].accountId);
        setDestAccountId(accList[2].accountId);
      }
    } catch (err: any) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
    generateNewIdempotencyKey();
  }, []);

  const generateNewIdempotencyKey = () => {
    setIdempotencyKey('idemp-' + Math.random().toString(36).substring(2, 12));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSubmitting(true);
      setErrorMsg('');
      setReplayNotice(null);

      const resp = await ApiClient.request<Transaction>('/transactions', {
        method: 'POST',
        headers: {
          'Idempotency-Key': idempotencyKey
        },
        body: JSON.stringify({
          sourceAccountId: sourceAccountId || null,
          destinationAccountId: destAccountId || null,
          amount: parseFloat(amount),
          currency,
          transactionType: txType,
          description: description || `Client submission ${idempotencyKey}`
        })
      });

      if (resp.idempotentReplay) {
        setReplayNotice(`Idempotent Replay: Transaction ${resp.transactionId} was previously processed with this key.`);
      } else {
        setShowSubmitModal(false);
        setAmount('');
        setDescription('');
        generateNewIdempotencyKey();
        await fetchTransactions();
      }
    } catch (err: any) {
      setErrorMsg(err.message || 'Transaction submission failed');
    } finally {
      setSubmitting(false);
    }
  };

  const handleReverse = async (tx: Transaction) => {
    if (!confirm(`Are you sure you want to reverse transaction ${tx.transactionId}?`)) return;
    try {
      await ApiClient.request(`/transactions/${tx.transactionId}/reverse`, {
        method: 'POST',
        body: JSON.stringify({ reason: 'Operational manual reversal requested' })
      });
      await fetchTransactions();
    } catch (err: any) {
      alert(`Reversal failed: ${err.message}`);
    }
  };

  const handleViewEvents = async (tx: Transaction) => {
    try {
      const events = await ApiClient.request<TransactionEvent[]>(`/transactions/${tx.transactionId}/history`);
      setSelectedTxEvents({ tx, events });
    } catch (err: any) {
      alert(`Failed to load event timeline: ${err.message}`);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-800 pb-5">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Transaction Ingestion Engine</h1>
          <p className="text-sm text-slate-400 mt-1">Guaranteed idempotency, distributed saga orchestration, and outbox event streaming.</p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={fetchTransactions}
            className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <button
            onClick={() => {
              generateNewIdempotencyKey();
              setShowSubmitModal(true);
            }}
            className="inline-flex items-center gap-2 px-3.5 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white font-medium text-xs transition shadow-sm"
          >
            <Plus className="w-4 h-4" />
            Submit Financial Transaction
          </button>
        </div>
      </div>

      {/* Transactions Table */}
      <div className="rounded-xl bg-slate-900 border border-slate-800 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-950 text-slate-400 font-semibold border-b border-slate-800">
              <tr>
                <th className="py-3 px-4">Transaction ID</th>
                <th className="py-3 px-4">Type</th>
                <th className="py-3 px-4">Amount</th>
                <th className="py-3 px-4">Idempotency Key</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Timestamp</th>
                <th className="py-3 px-4 text-center">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-mono">
              {transactions.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-slate-500 font-sans">
                    No transactions ingested yet.
                  </td>
                </tr>
              ) : (
                transactions.map((tx) => (
                  <tr key={tx.transactionId} className="hover:bg-slate-800/40 transition">
                    <td className="py-3 px-4 text-slate-300 truncate max-w-[150px]">{tx.transactionId}</td>
                    <td className="py-3 px-4 font-sans">
                      <span className="px-2 py-0.5 rounded text-[10px] font-medium bg-slate-800 text-slate-300 border border-slate-700">
                        {tx.transactionType}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-white font-semibold">
                      ${Number(tx.amount).toFixed(2)} <span className="text-slate-400 font-normal">{tx.currency}</span>
                    </td>
                    <td className="py-3 px-4 text-slate-400 truncate max-w-[120px]">{tx.idempotencyKey}</td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-sans font-medium ${
                          tx.status === 'POSTED'
                            ? 'bg-emerald-950/60 text-emerald-400 border border-emerald-800'
                            : tx.status === 'FAILED'
                            ? 'bg-rose-950/60 text-rose-400 border border-rose-800'
                            : tx.status === 'REVERSED'
                            ? 'bg-purple-950/60 text-purple-400 border border-purple-800'
                            : 'bg-amber-950/60 text-amber-400 border border-amber-800'
                        }`}
                      >
                        {tx.status === 'POSTED' && <CheckCircle2 className="w-3 h-3" />}
                        {tx.status === 'FAILED' && <XCircle className="w-3 h-3" />}
                        {tx.status === 'REVERSED' && <RotateCcw className="w-3 h-3" />}
                        {tx.status}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-slate-400 font-sans">
                      {new Date(tx.createdAt).toLocaleTimeString()}
                    </td>
                    <td className="py-3 px-4 text-center font-sans">
                      <div className="flex items-center justify-center gap-2">
                        <button
                          onClick={() => handleViewEvents(tx)}
                          title="View Saga & Event Timeline"
                          className="p-1.5 rounded hover:bg-slate-700 text-slate-300 text-xs transition"
                        >
                          <Layers className="w-3.5 h-3.5" />
                        </button>
                        {tx.status === 'POSTED' && (
                          <button
                            onClick={() => handleReverse(tx)}
                            title="Reverse Transaction"
                            className="p-1.5 rounded hover:bg-slate-700 text-purple-400 text-xs transition"
                          >
                            <RotateCcw className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Submit Transaction Modal */}
      {showSubmitModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-xl max-w-lg w-full p-6 space-y-5">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h2 className="text-base font-bold text-white flex items-center gap-2">
                <ArrowRightLeft className="w-4 h-4 text-indigo-400" />
                Submit Financial Transaction
              </h2>
              <button onClick={() => setShowSubmitModal(false)} className="text-slate-400 hover:text-white text-sm">
                ✕
              </button>
            </div>

            {replayNotice && (
              <div className="p-3 rounded-lg bg-amber-950/60 border border-amber-800 text-amber-300 text-xs">
                {replayNotice}
              </div>
            )}

            {errorMsg && (
              <div className="p-3 rounded-lg bg-rose-950/60 border border-rose-800 text-rose-300 text-xs">
                {errorMsg}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4 text-xs font-sans">
              <div>
                <label className="block text-slate-300 font-medium mb-1">Idempotency Key (Mandatory)</label>
                <div className="flex items-center gap-2">
                  <input
                    type="text"
                    required
                    value={idempotencyKey}
                    onChange={(e) => setIdempotencyKey(e.target.value)}
                    className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-white font-mono focus:outline-none focus:border-indigo-500"
                  />
                  <button
                    type="button"
                    onClick={generateNewIdempotencyKey}
                    className="px-2.5 py-2 rounded-lg bg-slate-800 text-slate-300 hover:bg-slate-700"
                    title="Generate New UUID Key"
                  >
                    🎲
                  </button>
                </div>
                <p className="text-[11px] text-slate-500 mt-1">Re-submitting with this exact key simulates idempotency replay or conflict validation.</p>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-300 font-medium mb-1">Source Account (Debit)</label>
                  <select
                    value={sourceAccountId}
                    onChange={(e) => setSourceAccountId(e.target.value)}
                    className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-white focus:outline-none focus:border-indigo-500"
                  >
                    <option value="">Select Account</option>
                    {accounts.map((a) => (
                      <option key={a.accountId} value={a.accountId}>
                        {a.accountNumber} ({a.accountName}) - ${Number(a.availableBalance).toFixed(0)}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-slate-300 font-medium mb-1">Destination Account (Credit)</label>
                  <select
                    value={destAccountId}
                    onChange={(e) => setDestAccountId(e.target.value)}
                    className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-white focus:outline-none focus:border-indigo-500"
                  >
                    <option value="">Select Account</option>
                    {accounts.map((a) => (
                      <option key={a.accountId} value={a.accountId}>
                        {a.accountNumber} ({a.accountName})
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-300 font-medium mb-1">Amount</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    placeholder="e.g. 5000.00"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-white font-mono focus:outline-none focus:border-indigo-500"
                  />
                </div>

                <div>
                  <label className="block text-slate-300 font-medium mb-1">Currency</label>
                  <select
                    value={currency}
                    onChange={(e) => setCurrency(e.target.value)}
                    className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-white focus:outline-none focus:border-indigo-500"
                  >
                    <option value="USD">USD ($)</option>
                    <option value="EUR">EUR (€)</option>
                    <option value="GBP">GBP (£)</option>
                    <option value="INR">INR (₹)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-slate-300 font-medium mb-1">Memo / Description</label>
                <input
                  type="text"
                  placeholder="e.g. Supplier invoice payment clearance"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-white focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div className="flex justify-end gap-3 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setShowSubmitModal(false)}
                  className="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-medium transition disabled:opacity-50"
                >
                  {submitting ? 'Executing Saga...' : 'Ingest & Process'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Event Timeline Drawer */}
      {selectedTxEvents && (
        <div className="fixed inset-0 z-50 flex items-center justify-end bg-black/70 backdrop-blur-sm p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-xl max-w-md w-full h-[85vh] p-6 flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                <h2 className="text-sm font-bold text-white">Transaction Lifecycle & Events</h2>
                <button onClick={() => setSelectedTxEvents(null)} className="text-slate-400 hover:text-white">✕</button>
              </div>

              <div className="mt-4 p-3 rounded-lg bg-slate-950 border border-slate-800 space-y-1 text-xs">
                <div className="text-slate-400">ID: <span className="font-mono text-white">{selectedTxEvents.tx.transactionId}</span></div>
                <div className="text-slate-400">Status: <span className="font-semibold text-emerald-400">{selectedTxEvents.tx.status}</span></div>
                <div className="text-slate-400">Amount: <span className="font-mono text-white">${selectedTxEvents.tx.amount} {selectedTxEvents.tx.currency}</span></div>
              </div>

              <div className="mt-6 space-y-4 overflow-y-auto max-h-[50vh] pr-2">
                <div className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Outbox & Audit Trail</div>
                {selectedTxEvents.events.length === 0 ? (
                  <p className="text-xs text-slate-500">No events recorded for this transaction.</p>
                ) : (
                  selectedTxEvents.events.map((ev, i) => (
                    <div key={ev.eventId} className="relative pl-6 pb-4 border-l-2 border-slate-800 last:border-transparent">
                      <div className="absolute -left-[9px] top-0 w-4 h-4 rounded-full bg-indigo-600 flex items-center justify-center text-[10px] text-white">
                        {i + 1}
                      </div>
                      <div className="text-xs font-semibold text-white">{ev.eventType}</div>
                      <div className="text-[11px] text-slate-400 mt-0.5">{ev.payload}</div>
                      <div className="text-[10px] text-slate-500 mt-1">{new Date(ev.createdAt).toLocaleString()}</div>
                    </div>
                  ))
                )}
              </div>
            </div>

            <button
              onClick={() => setSelectedTxEvents(null)}
              className="w-full py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium"
            >
              Close Drawer
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
