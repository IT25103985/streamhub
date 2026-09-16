import React, { useState, useEffect } from 'react';
import { reportService } from './reportService';
import AdminDataTable from '../../components/AdminDataTable';
import ReportDetailModal from './ReportDetailModal';
import './Report.css';

const REASON_MAP = {
  SPAM: 'Spam',
  HARASSMENT_OR_BULLYING: 'Harassment / Bullying',
  HATE_SPEECH: 'Hate Speech',
  SEXUAL_CONTENT: 'Sexual Content',
  VIOLENCE_OR_DANGEROUS: 'Violence / Dangerous',
  MISINFORMATION: 'Misinformation',
  COPYRIGHT: 'Copyright Issue',
  OTHER: 'Other'
};

//moderation queue for reviewing flagged videos comments and accounts
const AdminReportsPage = () => {
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [selectedReportId, setSelectedReportId] = useState(null);

  const fetchReports = async (isBackground = false) => {
    try {
      if (!isBackground) setLoading(true);
      const data = await reportService.getAllReportsAdmin(statusFilter, typeFilter, 0, 100);
      const reportList = data?.content || (Array.isArray(data) ? data : []);
      setReports(reportList);
      setError(null);
    } catch (err) {
      console.error('Error fetching reports:', err);
      if (!isBackground) setError('Failed to load reports. Please try again.');
    } finally {
      if (!isBackground) setLoading(false);
    }
  };

  useEffect(() => {
    fetchReports(false);

    const handleUpdate = () => fetchReports(true);
    window.addEventListener('streamhub-report-update', handleUpdate);
    window.addEventListener('focus', handleUpdate);
    document.addEventListener('visibilitychange', handleUpdate);

    const interval = setInterval(() => fetchReports(true), 10000);

    return () => {
      window.removeEventListener('streamhub-report-update', handleUpdate);
      window.removeEventListener('focus', handleUpdate);
      document.removeEventListener('visibilitychange', handleUpdate);
      clearInterval(interval);
    };
  }, [statusFilter, typeFilter]);

  const handleStatusChange = async (id, newStatus) => {
    if (!id) return;
    try {
      await reportService.updateReportStatusAdmin(id, newStatus);
      window.dispatchEvent(new CustomEvent('streamhub-report-update'));
      fetchReports(true);
    } catch (err) {
      console.error('Error updating report status:', err);
      alert('Failed to update report status.');
    }
  };

  const pendingCount = reports.filter(r => r.status === 'PENDING').length;

  const columns = [
    { 
      key: 'reportableType', 
      label: 'Type',
      render: (item) => (
        <span className="badge badge-type">
          {item.reportableType || 'UNKNOWN'}
        </span>
      )
    },
    { 
      key: 'contentPreview', 
      label: 'Reported Content',
      render: (item) => {
        const text = item.contentPreview || 'N/A';
        const truncated = text.length > 40 ? text.substring(0, 37) + '...' : text;
        return (
          <span 
            title={text} 
            style={{ fontWeight: 500, color: 'var(--text)', cursor: 'pointer' }}
            onClick={() => setSelectedReportId(item.id)}
          >
            {truncated}
          </span>
        );
      }
    },
    { 
      key: 'reason', 
      label: 'Reason',
      render: (item) => {
        const formatted = REASON_MAP[item.reason] || item.reason || 'Not specified';
        return <span style={{ color: 'var(--text-dim)' }}>{formatted}</span>;
      }
    },
    { 
      key: 'reporterName', 
      label: 'Reporter',
      render: (item) => (
        <span style={{ color: 'var(--text-dim)' }}>
          {item.reporterName || item.reporterUsername || 'Anonymous'}
        </span>
      )
    },
    { 
      key: 'status', 
      label: 'Status',
      render: (item) => {
        const status = item.status || 'PENDING';
        return (
          <span className={`badge badge-${status.toLowerCase()}`}>
            {status}
          </span>
        );
      }
    },
    { 
      key: 'createdAt', 
      label: 'Date',
      render: (item) => (
        <span style={{ color: 'var(--text-faint)', fontSize: '13px' }}>
          {item.createdAt ? new Date(item.createdAt).toLocaleDateString() : 'N/A'}
        </span>
      )
    }
  ];

  const actions = (item) => (
    <div className="admin-table-actions" onClick={(e) => e.stopPropagation()}>
      <select 
        value={item.status} 
        onChange={(e) => handleStatusChange(item.id, e.target.value)}
        style={{ 
          padding: '6px 8px', 
          borderRadius: '6px', 
          border: '1px solid var(--border)', 
          backgroundColor: 'var(--surface-2, #1f2937)', 
          color: 'var(--text, white)',
          fontSize: '12px',
          cursor: 'pointer'
        }}
      >
        <option value="PENDING">Pending</option>
        <option value="REVIEWED">Reviewed</option>
        <option value="RESOLVED">Resolved</option>
      </select>
      <button 
        className="btn-action"
        onClick={() => setSelectedReportId(item.id)}
        style={{ fontSize: '12px', padding: '6px 12px' }}
      >
        Review
      </button>
    </div>
  );

  return (
    <div className="admin-page">
      <div className="admin-page-header">
        <div>
          <h2>Report Moderation</h2>
          <p style={{ color: 'var(--text-dim)', fontSize: '14px', marginTop: '4px' }}>
            Review user-submitted violation flags and take moderation actions.
            {pendingCount > 0 && (
              <span className="badge badge-pending" style={{ marginLeft: '10px' }}>
                {pendingCount} Pending
              </span>
            )}
          </p>
        </div>
        <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
          <select 
            value={statusFilter} 
            onChange={(e) => setStatusFilter(e.target.value)}
            className="form-control"
            style={{ padding: '8px 12px', borderRadius: '6px', backgroundColor: 'var(--surface)', color: 'var(--text)', border: '1px solid var(--border)' }}
          >
            <option value="ALL">All Statuses</option>
            <option value="PENDING">Pending</option>
            <option value="REVIEWED">Reviewed</option>
            <option value="RESOLVED">Resolved</option>
          </select>
          <select 
            value={typeFilter} 
            onChange={(e) => setTypeFilter(e.target.value)}
            className="form-control"
            style={{ padding: '8px 12px', borderRadius: '6px', backgroundColor: 'var(--surface)', color: 'var(--text)', border: '1px solid var(--border)' }}
          >
            <option value="ALL">All Types</option>
            <option value="VIDEO">Videos</option>
            <option value="COMMENT">Comments</option>
            <option value="PLAYLIST">Playlists</option>
            <option value="PROFILE">Profiles</option>
          </select>
        </div>
      </div>

      {error && <div className="alert alert-error" style={{ marginBottom: '20px' }}>{error}</div>}

      <div className="admin-page-content">
        <AdminDataTable 
          data={reports} 
          columns={columns} 
          actions={actions}
          loading={loading}
          onRowClick={(row) => setSelectedReportId(row.id)}
        />
      </div>

      {selectedReportId && (
        <ReportDetailModal 
          reportId={selectedReportId}
          onClose={() => setSelectedReportId(null)}
          onActionComplete={() => {
            setSelectedReportId(null);
            window.dispatchEvent(new CustomEvent('streamhub-report-update'));
            window.dispatchEvent(new CustomEvent('streamhub-video-update'));
            fetchReports(false);
          }}
        />
      )}
    </div>
  );
};

export default AdminReportsPage;

