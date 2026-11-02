import React, { useEffect, useState } from 'react';
import { ApiClient } from '../api';
import { Account, JournalEntry, TrialBalance } from '../types';
import { BookOpen, CheckCircle2, AlertTriangle, RefreshCw, Layers, ArrowRight } from 'lucide-react';

export const LedgerView: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'journals' | 'trialBalance'>('trialBalance');
  const [trialBalance, setTrialBalance] = useState<TrialBalance | null>(null);
  const [journals, setJournals] = useState<JournalEntry[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchLedgerData = async () => {
    try {
      setLoading(true);
      const [tbData, accList] = await Promise.all([
        ApiClient.request<TrialBalance>('/ledger/trial-balance'),
        ApiClient.request<Account[]>('/accounts')
      ]);
      setTrialBalance(tbData);
      setAccounts(accList);
    } catch (err: any) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLedgerData();
  }, []);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-800 pb-5">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Double-Entry Ledger Engine</h1>
          <p className="text-sm text-slate-400 mt-1">Authoritative general ledger, immutable journal postings & mathematical trial balance.</p>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex bg-slate-900 border border-slate-800 rounded-lg p-1 text-xs">
            <button
              onClick={() => setActiveTab('trialBalance')}
              className={`px-3 py-1.5 rounded-md font-medium transition ${
                activeTab === 'trialBalance' ? 'bg-indigo-600 text-white' : 'text-slate-400 hover:text-white'
              }`}
            >
              Trial Balance Invariant
            </button>
            <button
              onClick={() => setActiveTab('journals')}
              className={`px-3 py-1.5 rounded-md font-medium transition ${
                activeTab === 'journals' ? 'bg-indigo-600 text-white' : 'text-slate-400 hover:text-white'
              }`}
            >
              Account Master Postings
            </button>
          </div>
          <button
            onClick={fetchLedgerData}
            className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Trial Balance Invariant Banner */}
      {trialBalance && (
        <div
          className={`p-4 rounded-xl border flex flex-col md:flex-row md:items-center justify-between gap-4 ${
            trialBalance.balanced
              ? 'bg-emerald-950/40 border-emerald-800/80 text-emerald-200'
              : 'bg-rose-950/40 border-rose-800/80 text-rose-200'
          }`}
        >
          <div className="flex items-center gap-3">
            {trialBalance.balanced ? (
              <div className="p-2 rounded-lg bg-emerald-900/60 text-emerald-400">
                <CheckCircle2 className="w-5 h-5" />
              </div>
            ) : (
              <div className="p-2 rounded-lg bg-rose-900/60 text-rose-400">
                <AlertTriangle className="w-5 h-5" />
              </div>
            )}
            <div>
              <div className="font-bold text-sm text-white">
                {trialBalance.balanced ? 'Accounting Invariant Satisfied: Balanced Ledger' : 'Accounting Invariant Alert: Unbalanced Ledger!'}
              </div>
              <div className="text-xs opacity-80 mt-0.5">
                Total Debits strictly equals Total Credits: &sum; Debits = ${Number(trialBalance.totalDebits).toLocaleString()} &equiv; &sum; Credits = ${Number(trialBalance.totalCredits).toLocaleString()}
              </div>
            </div>
          </div>
          <div className="flex items-center gap-6 font-mono text-xs">
            <div>
              <span className="text-slate-400 font-sans block text-[10px]">TOTAL DEBITS</span>
              <span className="font-bold text-emerald-400 text-sm">${Number(trialBalance.totalDebits).toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
            </div>
            <div>
              <span className="text-slate-400 font-sans block text-[10px]">TOTAL CREDITS</span>
              <span className="font-bold text-blue-400 text-sm">${Number(trialBalance.totalCredits).toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
            </div>
          </div>
        </div>
      )}

      {/* Tab 1: Trial Balance Table */}
      {activeTab === 'trialBalance' && trialBalance && (
        <div className="rounded-xl bg-slate-900 border border-slate-800 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-950 text-slate-400 font-semibold border-b border-slate-800">
                <tr>
                  <th className="py-3 px-4">Account Number</th>
                  <th className="py-3 px-4">Account Legal Title</th>
                  <th className="py-3 px-4">Type</th>
                  <th className="py-3 px-4 text-right">Debit Balance ($)</th>
                  <th className="py-3 px-4 text-right">Credit Balance ($)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono">
                {trialBalance.items.map((item) => (
                  <tr key={item.accountId} className="hover:bg-slate-800/40 transition">
                    <td className="py-3 px-4 text-indigo-400 font-semibold">{item.accountNumber}</td>
                    <td className="py-3 px-4 text-white font-sans">{item.accountName}</td>
                    <td className="py-3 px-4">
                      <span className="px-2 py-0.5 rounded text-[10px] font-sans font-medium bg-slate-800 text-slate-300 border border-slate-700">
                        {item.accountType}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-right text-emerald-400 font-semibold">
                      {Number(item.debitAmount) > 0 ? Number(item.debitAmount).toLocaleString(undefined, { minimumFractionDigits: 2 }) : '-'}
                    </td>
                    <td className="py-3 px-4 text-right text-blue-400 font-semibold">
                      {Number(item.creditAmount) > 0 ? Number(item.creditAmount).toLocaleString(undefined, { minimumFractionDigits: 2 }) : '-'}
                    </td>
                  </tr>
                ))}
              </tbody>
              <tfoot className="bg-slate-950 font-bold border-t border-slate-800 text-white font-mono">
                <tr>
                  <td colSpan={3} className="py-3 px-4 text-right font-sans uppercase tracking-wider text-xs text-slate-400">
                    Grand Trial Balance Sums:
                  </td>
                  <td className="py-3 px-4 text-right text-emerald-400">
                    ${Number(trialBalance.totalDebits).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                  </td>
                  <td className="py-3 px-4 text-right text-blue-400">
                    ${Number(trialBalance.totalCredits).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
      )}

      {/* Tab 2: Account Master Balances */}
      {activeTab === 'journals' && (
        <div className="rounded-xl bg-slate-900 border border-slate-800 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-950 text-slate-400 font-semibold border-b border-slate-800">
                <tr>
                  <th className="py-3 px-4">Account Number</th>
                  <th className="py-3 px-4">Account Name</th>
                  <th className="py-3 px-4">Classification</th>
                  <th className="py-3 px-4 text-right">Ledger Balance</th>
                  <th className="py-3 px-4 text-right">Available Balance</th>
                  <th className="py-3 px-4">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono">
                {accounts.map((acc) => (
                  <tr key={acc.accountId} className="hover:bg-slate-800/40 transition">
                    <td className="py-3 px-4 text-indigo-400 font-semibold">{acc.accountNumber}</td>
                    <td className="py-3 px-4 text-white font-sans">{acc.accountName}</td>
                    <td className="py-3 px-4 font-sans">{acc.accountType}</td>
                    <td className="py-3 px-4 text-right text-slate-200 font-semibold">
                      ${Number(acc.ledgerBalance).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </td>
                    <td className="py-3 px-4 text-right text-emerald-400 font-semibold">
                      ${Number(acc.availableBalance).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </td>
                    <td className="py-3 px-4 font-sans">
                      <span className="px-2 py-0.5 rounded text-[10px] bg-emerald-950/60 text-emerald-400 border border-emerald-800">
                        {acc.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};
