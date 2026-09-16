import React, { useState, useEffect } from 'react';
import { reportApi } from '../../api/reportApi';
import './Report.css';

//moderator dialog displaying report history and resolution form
const ReportDetailModal = ({ reportId, onClose, onActionComplete }) => {
    const [report, setReport] = useState(null);
    const [loading, setLoading] = useState(true);
    const [actionLoading, setActionLoading] = useState(false);
    const [error, setError] = useState(null);
    const [actionReason, setActionReason] = useState('');
    const [showReverseForm, setShowReverseForm] = useState(false);
    const [reverseReason, setReverseReason] = useState('');

    useEffect(() => {
        fetchReportDetails();
    }, [reportId]);

    const fetchReportDetails = async () => {
        try {
            setLoading(true);
            const data = await reportApi.getReport(reportId);
            setReport(data);
            setError(null);
        } catch (err) {
            console.error('Error fetching report details:', err);
            setError('Failed to load report details.');
        } finally {
            setLoading(false);
        }
    };

    const handleAction = async (actionType) => {
        if (!actionReason.trim()) {
            alert('Please provide a reason for this action.');
            return;
        }
        
        try {
            setActionLoading(true);
            await reportApi.takeAction(reportId, actionType, actionReason);
            alert(`Action '${actionType}' taken successfully.`);
            onActionComplete();
        } catch (err) {
            console.error('Error taking action:', err);
            const errorMsg = err.response?.data?.message || (typeof err.response?.data === 'string' ? err.response?.data : null) || 'Failed to take action.';
            alert(errorMsg);
        } finally {
            setActionLoading(false);
        }
    };

    const handleReverseAction = async () => {
        try {
            setActionLoading(true);
            await reportApi.reverseAction(reportId, reverseReason.trim());
            alert('Moderation action successfully reversed. Content restored.');
            setShowReverseForm(false);
            setReverseReason('');
            await fetchReportDetails();
            if (onActionComplete) onActionComplete();
        } catch (err) {
            console.error('Error reversing action:', err);
            const errorMsg = err.response?.data?.message || (typeof err.response?.data === 'string' ? err.response?.data : null) || 'Failed to reverse action.';
            alert(errorMsg);
        } finally {
            setActionLoading(false);
        }
    };

    if (loading) {
        return (
            <div className="modal-overlay">
                <div className="modal-content report-modal">
                    <p>Loading report details...</p>
                </div>
            </div>
        );
    }

    if (error || !report) {
        return (
            <div className="modal-overlay">
                <div className="modal-content report-modal">
                    <button className="modal-close" onClick={onClose}>&times;</button>
                    <p className="alert alert-error">{error || 'Report not found'}</p>
                </div>
            </div>
        );
    }

    const { contentSnapshot } = report;
    const isProfile = report.reportableType === 'PROFILE';
    const isResolved = report.status === 'RESOLVED';

    return (
        <div className="modal-overlay">
            <div className="modal-content report-modal" style={{ maxWidth: '600px', maxHeight: '90vh', overflowY: 'auto' }}>
                <button className="modal-close" onClick={onClose} aria-label="Close">&times;</button>
                <div className="modal-header">
                    <h2>Report Details</h2>
                </div>
                
                <div className="report-detail-section">
                    <h3>Reporter Info</h3>
                    <p><strong>Reporter:</strong> {report.reporterName}</p>
                    <p><strong>Date:</strong> {new Date(report.createdAt).toLocaleString()}</p>
                    <p><strong>Reason:</strong> <span className="badge badge-info">{report.reason}</span></p>
                    {report.details && (
                        <p><strong>Additional Details:</strong> {report.details}</p>
                    )}
                    <p><strong>Current Status:</strong> <span className={`badge badge-${report.status.toLowerCase()}`}>{report.status}</span></p>
                </div>

                <hr style={{ margin: '16px 0' }} />

                <div className="report-detail-section">
                    <h3>Content Snapshot ({report.reportableType})</h3>
                    {contentSnapshot && report.reportableType === 'VIDEO' && (
                        <div>
                            <p><strong>Title:</strong> {contentSnapshot.title}</p>
                            <p><strong>Uploader:</strong> {contentSnapshot.uploader}</p>
                        </div>
                    )}
                    {contentSnapshot && report.reportableType === 'COMMENT' && (
                        <div>
                            <p><strong>Text:</strong> {contentSnapshot.text}</p>
                            <p><strong>Author:</strong> {contentSnapshot.author}</p>
                            <p><strong>On Video:</strong> {contentSnapshot.videoTitle}</p>
                        </div>
                    )}
                    {contentSnapshot && report.reportableType === 'PLAYLIST' && (
                        <div>
                            <p><strong>Name:</strong> {contentSnapshot.name}</p>
                            <p><strong>Owner:</strong> {contentSnapshot.owner}</p>
                            <p><strong>Videos:</strong> {contentSnapshot.videoCount}</p>
                        </div>
                    )}
                    {contentSnapshot && report.reportableType === 'PROFILE' && (
                        <div>
                            <p><strong>Username:</strong> {contentSnapshot.username}</p>
                            <p><strong>Email:</strong> {contentSnapshot.email}</p>
                            <p><strong>Joined:</strong> {new Date(contentSnapshot.joinDate).toLocaleDateString()}</p>
                        </div>
                    )}
                    {!contentSnapshot && <p><em>Content snapshot unavailable (may have been deleted)</em></p>}
                </div>

                <hr style={{ margin: '16px 0' }} />

                <div className="report-detail-section">
                    <h3>Moderation Action</h3>
                    {isResolved ? (
                        <div>
                            <div className="alert alert-info">
                                <p><strong>Action Taken:</strong> {report.moderatorAction || 'None'}</p>
                                <p><strong>Reason Given:</strong> {report.actionReason || 'N/A'}</p>
                                {report.resolvedAt && (
                                    <p><strong>Resolved At:</strong> {new Date(report.resolvedAt).toLocaleString()}</p>
                                )}
                            </div>

                            {showReverseForm ? (
                                <div className="modal-form" style={{ marginTop: '16px', padding: '14px', border: '1px solid var(--border)', borderRadius: '8px', background: 'rgba(255,255,255,0.02)' }}>
                                    <h4 style={{ margin: '0 0 8px 0', color: 'var(--amber, #f59e0b)' }}>Reverse Moderation Action</h4>
                                    <p style={{ fontSize: '13px', color: 'var(--text-secondary, #9aa3c0)', marginBottom: '12px' }}>
                                        Reversing this action will restore any hidden content and return the report to PENDING status.
                                    </p>
                                    <div className="form-group">
                                        <label htmlFor="reverseReason">Reason for Reversal (Optional)</label>
                                        <textarea
                                            id="reverseReason"
                                            value={reverseReason}
                                            onChange={(e) => setReverseReason(e.target.value)}
                                            className="form-control"
                                            rows="2"
                                            placeholder="Explain why this action is being reversed (e.g. false flag, appeal approved)..."
                                            disabled={actionLoading}
                                        />
                                    </div>
                                    <div className="form-actions" style={{ justifyContent: 'flex-start', gap: '8px', marginTop: '12px' }}>
                                        <button
                                            className="btn btn-warning"
                                            onClick={handleReverseAction}
                                            disabled={actionLoading}
                                        >
                                            {actionLoading ? 'Reversing...' : 'Confirm Reverse Action'}
                                        </button>
                                        <button
                                            className="btn btn-secondary"
                                            onClick={() => setShowReverseForm(false)}
                                            disabled={actionLoading}
                                        >
                                            Cancel
                                        </button>
                                    </div>
                                </div>
                            ) : (
                                <div style={{ marginTop: '12px' }}>
                                    <button
                                        className="btn btn-warning"
                                        onClick={() => setShowReverseForm(true)}
                                    >
                                        ↺ Reverse Action / Restore Content
                                    </button>
                                </div>
                            )}
                        </div>
                    ) : (
                        <div className="modal-form">
                            <div className="form-group">
                                <label htmlFor="actionReason">Action Reason (Required)</label>
                                <textarea
                                    id="actionReason"
                                    value={actionReason}
                                    onChange={(e) => setActionReason(e.target.value)}
                                    className="form-control"
                                    rows="3"
                                    placeholder="Explain the moderation action taken..."
                                    disabled={actionLoading}
                                />
                            </div>
                            
                            <div className="form-actions" style={{ justifyContent: 'flex-start', flexWrap: 'wrap' }}>
                                {!isProfile && (
                                    <>
                                        <button 
                                            className="btn btn-warning" 
                                            onClick={() => handleAction('HIDDEN')}
                                            disabled={actionLoading || !actionReason.trim()}
                                        >
                                            Hide Content
                                        </button>
                                        <button 
                                            className="btn btn-danger" 
                                            onClick={() => handleAction('DELETED')}
                                            disabled={actionLoading || !actionReason.trim()}
                                        >
                                            Delete Content
                                        </button>
                                    </>
                                )}
                                
                                {isProfile && (
                                    <button 
                                        className="btn btn-warning" 
                                        onClick={() => handleAction('ESCALATED')}
                                        disabled={actionLoading || !actionReason.trim()}
                                    >
                                        Escalate to Administrator
                                    </button>
                                )}

                                <button 
                                    className="btn btn-secondary" 
                                    onClick={() => handleAction('NONE')}
                                    disabled={actionLoading || !actionReason.trim()}
                                >
                                    Dismiss (No Action)
                                </button>
                            </div>
                        </div>
                    )}
                </div>
                
                {isResolved && (
                    <div className="form-actions" style={{ marginTop: '16px' }}>
                        <button className="btn btn-secondary" onClick={onClose}>Close</button>
                    </div>
                )}
            </div>
        </div>
    );
};

export default ReportDetailModal;
