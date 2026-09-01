import React, { useState, useEffect } from 'react';
import { reportApi } from '../../api/reportApi';
import AdminDataTable from '../../components/AdminDataTable';
import ReportDetailModal from '../../features/report/ReportDetailModal';

//admin moderation page listing submitted platform reports
const ReportsPage = () => {
    const [reports, setReports] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [statusFilter, setStatusFilter] = useState('');
    const [typeFilter, setTypeFilter] = useState('');
    
      const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(1);
    
    const [selectedReportId, setSelectedReportId] = useState(null);

    useEffect(() => {
        fetchReports();
    }, [page, statusFilter, typeFilter]);

    const fetchReports = async () => {
        try {
            setLoading(true);
            const data = await reportApi.getReports(statusFilter || undefined, typeFilter || undefined, page, 10);
            setReports(data.content || []);
            setTotalPages(data.totalPages || 1);
            setError(null);
        } catch (err) {
            console.error('Error fetching reports:', err);
            setError('Failed to load reports.');
        } finally {
            setLoading(false);
        }
    };

    const handleFilterChange = (setter) => (e) => {
        setter(e.target.value);
        setPage(0); //reset page offset on filter switch
    };

    const columns = [
        { key: 'reportableType', label: 'Type' },
        { key: 'reason', label: 'Reason' },
        { 
            key: 'contentPreview', 
            label: 'Reported Content',
            render: (value) => value && value.length > 30 ? value.substring(0, 30) + '...' : value
        },
        { key: 'reporterName', label: 'Reporter' },
        { 
            key: 'status', 
            label: 'Status',
            render: (value) => (
                <span className={`badge badge-${value.toLowerCase()}`}>
                    {value}
                </span>
            )
        },
        { 
            key: 'createdAt', 
            label: 'Date',
            render: (value) => new Date(value).toLocaleDateString()
        }
    ];

    const actions = [
        {
            label: 'Review',
            onClick: (report) => setSelectedReportId(report.id),
            className: 'btn-secondary btn-sm'
        }
    ];

    return (
        <div className="admin-page-container">
            <div className="admin-page-header">
                <h1>Report Management</h1>
                <div className="admin-filters" style={{ display: 'flex', gap: '10px' }}>
                    <select 
                        value={statusFilter} 
                        onChange={handleFilterChange(setStatusFilter)}
                        className="form-control"
                    >
                        <option value="">All Statuses</option>
                        <option value="PENDING">Pending</option>
                        <option value="REVIEWED">Reviewed</option>
                        <option value="RESOLVED">Resolved</option>
                    </select>

                    <select 
                        value={typeFilter} 
                        onChange={handleFilterChange(setTypeFilter)}
                        className="form-control"
                    >
                        <option value="">All Types</option>
                        <option value="VIDEO">Video</option>
                        <option value="COMMENT">Comment</option>
                        <option value="PLAYLIST">Playlist</option>
                        <option value="PROFILE">Profile</option>
                    </select>
                </div>
            </div>

            {error && <div className="alert alert-error">{error}</div>}

            <AdminDataTable
                data={reports}
                columns={columns}
                actions={actions}
                loading={loading}
            />

            <div className="pagination-controls" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '20px' }}>
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

            {selectedReportId && (
                <ReportDetailModal
                    reportId={selectedReportId}
                    onClose={() => setSelectedReportId(null)}
                    onActionComplete={() => {
                        setSelectedReportId(null);
                        fetchReports();
                    }}
                />
            )}
        </div>
    );
};

export default ReportsPage;
