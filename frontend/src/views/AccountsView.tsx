import React, { useEffect, useState } from 'react';
import { ApiClient } from '../api';
import { Account, AccountType } from '../types';
import { Plus, ShieldAlert, ShieldCheck, Lock, Unlock, Search, RefreshCw, FileText } from 'lucide-react';

export const AccountsView: React.FC = () => {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [activeStatementAccount, setActiveStatementAccount] = useState<Account | null>(null);

  // Form states
  const [accountName, setAccountName] = useState('');
  const [customerRef, setCustomerRef] = useState('');
  const [accountType, setAccountType] = useState<AccountType>('LIABILITY');
  const [currency, setCurrency] = useState('USD');
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const fetchAccounts = async () => {
    try {
      setLoading(true);
      const data = await ApiClient.request<Account[]>('/accounts');
      setAccounts(data);
    } catch (err: any) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAccounts();
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSubmitting(true);
      setErrorMsg('');
      await ApiClient.request('/accounts', {
        method: 'POST',
        body: JSON.stringify({
          accountName,
          customerReference: customerRef,
          accountType,
          currency
        })
      });
      setShowCreateModal(false);
      setAccountName('');
      setCustomerRef('');
      await fetchAccounts();
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to create account');
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggleFreeze = async (acc: Account) => {
    const isFrozen = acc.status === 'FROZEN';
    const action = isFrozen ? 'unfreeze' : 'freeze';
    if (!confirm(`Are you sure you want to ${action} account ${acc.accountNumber}?`)) return;

    try {
      await ApiClient.request(`/accounts/${acc.accountId}/${action}`, {
        method: 'POST',
        body: JSON.stringify({ reason: `Administrative ${action} via operations console` })
      });
      await fetchAccounts();
    } catch (err: any) {
      alert(`Action failed: ${err.message}`);
    }
  };

  const filtered = accounts.filter(
    (a) =>
      a.accountName.toLowerCase().includes(search.toLowerCase()) ||
      a.accountNumber.toLowerCase().includes(search.toLowerCase()) ||
      a.customerReference.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-800 pb-5">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Financial Account Master</h1>
          <p className="text-sm text-slate-400 mt-1">Manage asset, liability and expense accounts with authoritative balance segregation.</p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={fetchAccounts}
            className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <button
            onClick={() => setShowCreateModal(true)}
            className="inline-flex items-center gap-2 px-3.5 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white font-medium text-xs transition shadow-sm"
          >
            <Plus className="w-4 h-4" />
            Create Financial Account
          </button>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="flex items-center gap-3 bg-slate-900 p-3 rounded-lg border border-slate-800">
        <Search className="w-4 h-4 text-slate-400" />
        <input
          type="text"
          placeholder="Search by account number, name, or customer reference..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="bg-transparent text-xs text-white placeholder-slate-500 focus:outline-none w-full"
        />
      </div>

      {/* Accounts Data Table */}
      <div className="rounded-xl bg-slate-900 border border-slate-800 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-950 text-slate-400 font-semibold border-b border-slate-800">
              <tr>
                <th className="py-3 px-4">Account Number</th>
                <th className="py-3 px-4">Account Name</th>
                <th className="py-3 px-4">Type</th>
                <th className="py-3 px-4">Currency</th>
                <th className="py-3 px-4 text-right">Available Balance</th>
                <th className="py-3 px-4 text-right">Ledger Balance</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4 text-center">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-mono">
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-8 text-center text-slate-500 font-sans">
                    No matching accounts found.
                  </td>
                </tr>
              ) : (
                filtered.map((acc) => (
                  <tr key={acc.accountId} className="hover:bg-slate-800/40 transition">
                    <td className="py-3 px-4 text-indigo-400 font-semibold">{acc.accountNumber}</td>
                    <td className="py-3 px-4 text-white font-sans">{acc.accountName}</td>
                    <td className="py-3 px-4">
                      <span className="px-2 py-0.5 rounded text-[10px] font-sans font-medium bg-slate-800 text-slate-300 border border-slate-700">
                        {acc.accountType}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-slate-400">{acc.currency}</td>
                    <td className="py-3 px-4 text-right text-emerald-400 font-semibold">
                      ${Number(acc.availableBalance).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    </td>
                    <td className="py-3 px-4 text-right text-slate-200 font-semibold">
                      ${Number(acc.ledgerBalance).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-sans font-medium ${
                          acc.status === 'ACTIVE'
                            ? 'bg-emerald-950/60 text-emerald-400 border border-emerald-800'
                            : 'bg-rose-950/60 text-rose-400 border border-rose-800'
                        }`}
                      >
                        {acc.status}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-center font-sans">
                      <div className="flex items-center justify-center gap-2">
                        <button
                          onClick={() => handleToggleFreeze(acc)}
                          title={acc.status === 'ACTIVE' ? 'Freeze Account' : 'Unfreeze Account'}
                          className={`p-1.5 rounded hover:bg-slate-700 text-xs transition ${
                            acc.status === 'ACTIVE' ? 'text-amber-400' : 'text-emerald-400'
                          }`}
                        >
                          {acc.status === 'ACTIVE' ? <Lock className="w-3.5 h-3.5" /> : <Unlock className="w-3.5 h-3.5" />}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Create Account Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-xl max-w-md w-full p-6 space-y-5">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h2 className="text-base font-bold text-white">Create Financial Account</h2>
              <button onClick={() => setShowCreateModal(false)} className="text-slate-400 hover:text-white text-sm">
                ✕
              </button>
            </div>

            {errorMsg && (
              <div className="p-3 rounded-lg bg-rose-950/60 border border-rose-800 text-rose-300 text-xs">
                {errorMsg}
              </div>
            )}

            <form onSubmit={handleCreate} className="space-y-4 text-xs font-sans">
              <div>
                <label className="block text-slate-300 font-medium mb-1">Account Legal Name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Apex Operations Account"
                  value={accountName}
                  onChange={(e) => setAccountName(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-white focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="block text-slate-300 font-medium mb-1">Customer / Entity Reference</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. CUST-CORP-99"
                  value={customerRef}
                  onChange={(e) => setCustomerRef(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-white focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-300 font-medium mb-1">Account Type</label>
                  <select
                    value={accountType}
                    onChange={(e) => setAccountType(e.target.value as AccountType)}
                    className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-white focus:outline-none focus:border-indigo-500"
                  >
                    <option value="LIABILITY">LIABILITY (Customer Deposit)</option>
                    <option value="ASSET">ASSET (Cash Settlement)</option>
                    <option value="EQUITY">EQUITY</option>
                    <option value="REVENUE">REVENUE</option>
                    <option value="EXPENSE">EXPENSE</option>
                  </select>
                </div>

                <div>
                  <label className="block text-slate-300 font-medium mb-1">Currency Code</label>
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

              <div className="flex justify-end gap-3 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-medium transition disabled:opacity-50"
                >
                  {submitting ? 'Creating...' : 'Confirm Account'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
