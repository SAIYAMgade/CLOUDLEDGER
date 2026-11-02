import React, { useState, useEffect } from 'react';
import { ApiClient } from './api';
import { DashboardView } from './views/DashboardView';
import { AccountsView } from './views/AccountsView';
import { TransactionsView } from './views/TransactionsView';
import { LedgerView } from './views/LedgerView';
import { SagaMonitorView } from './views/SagaMonitorView';
import { AuditView } from './views/AuditView';
import { ReportsView } from './views/ReportsView';
import { OperationsView } from './views/OperationsView';
import { LoginView } from './views/LoginView';
import {
  LayoutDashboard,
  Wallet,
  ArrowRightLeft,
  BookOpen,
  GitBranch,
  ShieldCheck,
  FileSpreadsheet,
  Activity,
  LogOut,
  Shield
} from 'lucide-react';

type NavTab = 'dashboard' | 'accounts' | 'transactions' | 'ledger' | 'sagas' | 'audit' | 'reports' | 'operations';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<NavTab>('dashboard');
  const [currentUser, setCurrentUser] = useState<any>(null);
  const [authenticated, setAuthenticated] = useState<boolean>(false);

  useEffect(() => {
    const token = ApiClient.getToken();
    if (token) {
      setAuthenticated(true);
      setCurrentUser({ username: 'Operator', roles: ['ROLE_ADMINISTRATOR'] });
    }

    const handleUnauthorized = () => {
      setAuthenticated(false);
      setCurrentUser(null);
    };

    window.addEventListener('auth-unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth-unauthorized', handleUnauthorized);
  }, []);

  const handleLogout = () => {
    ApiClient.setToken(null);
    setAuthenticated(false);
    setCurrentUser(null);
  };

  if (!authenticated) {
    return (
      <LoginView
        onLoginSuccess={(user) => {
          setAuthenticated(true);
          setCurrentUser(user);
        }}
      />
    );
  }

  const navItems = [
    { id: 'dashboard', label: 'Executive Dashboard', icon: LayoutDashboard },
    { id: 'accounts', label: 'Financial Accounts', icon: Wallet },
    { id: 'transactions', label: 'Transaction Ingestion', icon: ArrowRightLeft },
    { id: 'ledger', label: 'Double-Entry Ledger', icon: BookOpen },
    { id: 'sagas', label: 'Saga Workflow Monitor', icon: GitBranch },
    { id: 'audit', label: 'Audit & Hash-Chain', icon: ShieldCheck },
    { id: 'reports', label: 'Reports & CSV Export', icon: FileSpreadsheet },
    { id: 'operations', label: 'System Operations', icon: Activity }
  ];

  return (
    <div className="flex min-h-screen bg-slate-950 text-slate-100">
      {/* Sidebar Navigation */}
      <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col justify-between shrink-0">
        <div>
          {/* Brand header */}
          <div className="p-5 border-b border-slate-800 flex items-center gap-3">
            <div className="p-2 rounded-lg bg-indigo-600 text-white">
              <Shield className="w-5 h-5" />
            </div>
            <div>
              <div className="font-bold text-sm tracking-tight text-white">CloudLedger</div>
              <div className="text-[10px] text-slate-400 font-mono tracking-wider">ENTERPRISE PLATFORM</div>
            </div>
          </div>

          {/* Navigation Links */}
          <nav className="p-3 space-y-1">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => setActiveTab(item.id as NavTab)}
                  className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-xs font-medium transition ${
                    isActive
                      ? 'bg-indigo-600 text-white font-semibold shadow'
                      : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
                  }`}
                >
                  <Icon className="w-4 h-4 shrink-0" />
                  <span>{item.label}</span>
                </button>
              );
            })}
          </nav>
        </div>

        {/* User Session Footer */}
        <div className="p-4 border-t border-slate-800 flex items-center justify-between">
          <div className="text-xs">
            <div className="font-semibold text-white truncate max-w-[130px]">
              {currentUser?.username || 'Operator'}
            </div>
            <div className="text-[10px] text-emerald-400 font-medium">Session Active</div>
          </div>
          <button
            onClick={handleLogout}
            title="Sign Out"
            className="p-2 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-white transition"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="flex-1 overflow-y-auto p-6 md:p-8">
        <div className="max-w-7xl mx-auto">
          {activeTab === 'dashboard' && <DashboardView />}
          {activeTab === 'accounts' && <AccountsView />}
          {activeTab === 'transactions' && <TransactionsView />}
          {activeTab === 'ledger' && <LedgerView />}
          {activeTab === 'sagas' && <SagaMonitorView />}
          {activeTab === 'audit' && <AuditView />}
          {activeTab === 'reports' && <ReportsView />}
          {activeTab === 'operations' && <OperationsView />}
        </div>
      </main>
    </div>
  );
};

export default App;
