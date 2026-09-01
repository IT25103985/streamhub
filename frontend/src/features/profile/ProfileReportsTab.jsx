import React, { useState, useEffect } from 'react';
import { reportApi } from '../../api/reportApi';

//tab showing moderation reports submitted by the user
const ProfileReportsTab = () => {
    const [reports, setReports] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(1);

    useEffect(() => {
        fetchReports();
    }, [page]);

    const fetchReports = async () => {
        try {
            setLoading(true);
            const data = await reportApi.getMyReports(page, 10);
            setReports(data.content || []);
            setTotalPages(data.totalPages || 1);
            setError(null);
        } catch (err) {
            console.error('Error fetching reports:', err);
            setError('Failed to load your reports.');
        } finally {
            setLoading(false);
        }
    };

    if (loading && page === 0) {
        return <div>Loading reports...</div>;
    }

    if (error) {
        return <div className="alert alert-error">{error}</div>;
    }

    if (reports.length === 0) {
        return <div>You haven't reported anything yet.</div>;
    }

    return (
        <div className="profile-reports-container">
            <div className="reports-list" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                {reports.map(report => (
                    <div key={report.id} className="report-card" style={{ border: '1px solid var(--border-color)', borderRadius: '8px', padding: '16px' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '12px' }}>
                            <div>
                                <span className={`badge badge-${report.status.toLowerCase()}`} style={{ marginRight: '8px' }}>
                                    {report.status}
                                </span>
                                <strong>{report.reportableType}</strong> - <span style={{ color: 'var(--text-secondary)' }}>{report.reason}</span>
                            </div>
                            <small style={{ color: 'var(--text-secondary)' }}>{new Date(report.createdAt).toLocaleDateString()}</small>
                        </div>
                        
                        <p style={{ margin: '8px 0', fontSize: '14px' }}>
                            <strong>Reported Content:</strong> {report.contentPreview || 'Unknown'}
                        </p>
                        
                        {report.status === 'RESOLVED' && (
                            <div style={{ marginTop: '12px', padding: '12px', backgroundColor: 'var(--bg-secondary)', borderRadius: '4px' }}>
                                <p style={{ margin: '0 0 4px 0', fontSize: '14px' }}><strong>Moderator Action:</strong> {report.moderatorAction}</p>
                                {report.actionReason && (
                                    <p style={{ margin: 0, fontSize: '13px', color: 'var(--text-secondary)' }}>{report.actionReason}</p>
                                )}
                            </div>
                        )}
                    </div>
                ))}
            </div>

            {totalPages > 1 && (
                <div className="pagination-controls" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '24px' }}>
                    <button 
                        className="btn btn-secondary btn-sm" 
                        disabled={page === 0 || loading}
                        onClick={() => setPage(p => p - 1)}
                    >
                        Previous
                    </button>
                    <span>Page {page + 1} of {totalPages}</span>
                    <button 
                        className="btn btn-secondary btn-sm" 
                        disabled={page >= totalPages - 1 || loading}
                        onClick={() => setPage(p => p + 1)}
                    >
                        Next
                    </button>
                </div>
            )}
        </div>
    );
};

export default ProfileReportsTab;
