import React, { useState, useEffect, useContext } from 'react';
import { Link } from 'react-router-dom';
import { AuthContext, useRequireAuth } from '../context/AuthContext';
import { reportApi } from '../api/reportApi';
import '../features/report/Report.css';

//user submitted reports tracking page showing status and resolution history
const MyReportsPage = () => {
  const { user } = useContext(AuthContext);
  const requireAuth = useRequireAuth();
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const fetchReports = async (isBackground = false) => {
    if (!user) return;
    try {
      if (!isBackground) setLoading(true);
      const data = await reportApi.getMyReports(page, 10);
      setReports(data.content || []);
      setTotalPages(data.totalPages || 1);
      setError(null);
    } catch (err) {
      console.error('Error fetching my reports:', err);
      if (!isBackground) setError('Failed to load your submitted reports.');
    } finally {
      if (!isBackground) setLoading(false);
    }
  };

  useEffect(() => {
    if (user) {
      fetchReports(false);
    } else {
      setLoading(false);
    }
  }, [user, page]);

  //live update synchronization
  useEffect(() => {
    if (!user) return;
    const handleUpdate = () => fetchReports(true);
    window.addEventListener('streamhub-report-update', handleUpdate);
    window.addEventListener('focus', handleUpdate);

    return () => {
      window.removeEventListener('streamhub-report-update', handleUpdate);
      window.removeEventListener('focus', handleUpdate);
    };
  }, [user, page]);

  if (!user) {
    return (
      <div style={{ padding: '40px 24px', maxWidth: '1000px', margin: '0 auto', textAlign: 'center' }}>
        <p style={{ color: 'var(--text-dim)', fontSize: '16px' }}>Please log in to view your submitted reports.</p>
        <Link to="/" className="btn btn-primary" style={{ display: 'inline-block', marginTop: '12px' }}>
          Back to Home
        </Link>
      </div>
    );
  }

  return (
    <div style={{ padding: '32px 24px 60px', maxWidth: '1000px', margin: '0 auto', minHeight: 'calc(100vh - 140px)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '28px', borderBottom: '1px solid var(--border)', paddingBottom: '16px' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: 700, margin: '0 0 6px 0', color: 'var(--text)' }}>My Reports</h1>
          <p style={{ fontSize: '14px', color: 'var(--text-dim)', margin: 0 }}>
            Track the status and outcome of violation reports you have submitted.
          </p>
        </div>
        <button 
          onClick={() => fetchReports(false)}
          className="btn btn-secondary btn-sm"
          style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <polyline points="23 4 23 10 17 10" />
            <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
          </svg>
          Refresh
        </button>
      </div>

      {error && (
        <div className="alert alert-error" style={{ marginBottom: '20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <span>{error}</span>
          <button className="btn btn-secondary btn-sm" onClick={() => fetchReports(false)}>Try Again</button>
        </div>
      )}

      {loading && page === 0 ? (
        <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--text-dim)' }}>
          Loading your reports...
        </div>
      ) : reports.length === 0 ? (
        <div style={{ 
          textAlign: 'center', 
          padding: '60px 24px', 
          background: 'var(--surface)', 
          border: '1px solid var(--border)', 
          borderRadius: '12px' 
        }}>
          <div style={{ fontSize: '40px', marginBottom: '12px' }}>🛡️</div>
          <h3 style={{ fontSize: '18px', fontWeight: 600, margin: '0 0 8px 0', color: 'var(--text)' }}>No Reports Submitted</h3>
          <p style={{ fontSize: '14px', color: 'var(--text-dim)', maxWidth: '400px', margin: '0 auto 20px' }}>
            You haven't submitted any reports yet. Reports you submit on videos, comments, or playlists will appear here.
          </p>
          <Link to="/" className="btn btn-primary">
            Explore Content
          </Link>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {reports.map((report) => {
            const isResolved = report.status === 'RESOLVED';
            const isReviewed = report.status === 'REVIEWED';
            const statusColor = isResolved 
              ? 'var(--emerald, #10b981)' 
              : isReviewed 
                ? 'var(--cyan, #06b6d4)' 
                : 'var(--amber, #f59e0b)';

            return (
              <div 
                key={report.id} 
                style={{ 
                  background: 'var(--surface)', 
                  border: '1px solid var(--border)', 
                  borderRadius: '10px', 
                  padding: '20px',
                  boxShadow: 'var(--shadow-sm)'
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px', flexWrap: 'wrap', gap: '8px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <span 
                      style={{ 
                        padding: '3px 9px', 
                        borderRadius: '6px', 
                        fontSize: '11px', 
                        fontWeight: 700, 
                        letterSpacing: '0.04em',
                        textTransform: 'uppercase',
                        background: 'rgba(255, 255, 255, 0.08)',
                        color: 'var(--text)'
                      }}
                    >
                      {report.reportableType}
                    </span>
                    <span 
                      style={{ 
                        padding: '3px 9px', 
                        borderRadius: '6px', 
                        fontSize: '11px', 
                        fontWeight: 700, 
                        background: `${statusColor}1a`, 
                        color: statusColor,
                        border: `1px solid ${statusColor}40`
                      }}
                    >
                      {report.status}
                    </span>
                  </div>
                  <span style={{ fontSize: '12px', color: 'var(--text-dim)' }}>
                    Submitted {new Date(report.createdAt).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                  </span>
                </div>

                <div style={{ marginBottom: '10px' }}>
                  <span style={{ fontSize: '13px', color: 'var(--text-dim)', marginRight: '6px' }}>Reason:</span>
                  <span style={{ fontSize: '14px', fontWeight: 600, color: 'var(--text)' }}>
                    {report.reason}
                  </span>
                </div>

                <div style={{ marginBottom: '10px', background: 'var(--void)', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--border)' }}>
                  <div style={{ fontSize: '12px', color: 'var(--text-dim)', marginBottom: '3px' }}>Reported Content</div>
                  <div style={{ fontSize: '14px', color: 'var(--text)', fontWeight: 500 }}>
                    {report.contentPreview || 'Content'}
                  </div>
                </div>

                {report.details && (
                  <div style={{ fontSize: '13px', color: 'var(--text-dim)', marginBottom: '12px', lineHeight: 1.4 }}>
                    <strong style={{ color: 'var(--text)' }}>Additional details:</strong> {report.details}
                  </div>
                )}

                {isResolved && (
                  <div style={{ 
                    marginTop: '14px', 
                    padding: '12px 14px', 
                    backgroundColor: 'rgba(16, 185, 129, 0.08)', 
                    border: '1px solid rgba(16, 185, 129, 0.25)', 
                    borderRadius: '8px' 
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                      <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--emerald, #10b981)' }}>
                        ✓ Moderation Outcome: {report.moderatorAction || 'Reviewed'}
                      </span>
                    </div>
                    {report.actionReason && (
                      <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--text-dim)', lineHeight: 1.4 }}>
                        {report.actionReason}
                      </p>
                    )}
                  </div>
                )}
              </div>
            );
          })}

          {totalPages > 1 && (
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '20px' }}>
              <button 
                className="btn btn-secondary btn-sm" 
                disabled={page === 0 || loading}
                onClick={() => setPage(p => p - 1)}
              >
                Previous Page
              </button>
              <span style={{ fontSize: '13px', color: 'var(--text-dim)' }}>
                Page {page + 1} of {totalPages}
              </span>
              <button 
                className="btn btn-secondary btn-sm" 
                disabled={page >= totalPages - 1 || loading}
                onClick={() => setPage(p => p + 1)}
              >
                Next Page
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default MyReportsPage;
