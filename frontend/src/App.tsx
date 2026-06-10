import { useState } from 'react';
import ReportForm from './components/ReportForm';
import ReportsList from './components/ReportsList';
import QueryView from './components/QueryView';
import './App.css';

type Tab = 'submit' | 'reports' | 'query';

function App() {
  const [tab, setTab] = useState<Tab>('submit');

  return (
    <div className="app">
      <header className="app-header">
        <h1>🔍 FrictionLens</h1>
        <p className="tagline">Surface workplace friction. Fix what slows you down.</p>
        <nav className="tabs">
          <button className={tab === 'submit' ? 'active' : ''} onClick={() => setTab('submit')}>
            Report
          </button>
          <button className={tab === 'reports' ? 'active' : ''} onClick={() => setTab('reports')}>
            Browse
          </button>
          <button className={tab === 'query' ? 'active' : ''} onClick={() => setTab('query')}>
            Ask AI
          </button>
        </nav>
      </header>

      <main className="app-main">
        {tab === 'submit' && <ReportForm />}
        {tab === 'reports' && <ReportsList />}
        {tab === 'query' && <QueryView />}
      </main>
    </div>
  );
}

export default App;

