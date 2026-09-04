import React, { useState } from 'react';
import ReactDOM from 'react-dom';
import { reportApi } from '../../api/reportApi';
import './Report.css';

const REASON_LIST = [
    { value: 'SPAM', label: 'Spam' },
    { value: 'HARASSMENT_OR_BULLYING', label: 'Harassment or bullying' },
    { value: 'HATE_SPEECH', label: 'Hate speech' },
    { value: 'SEXUAL_CONTENT', label: 'Sexual content' },
    { value: 'VIOLENCE_OR_DANGEROUS', label: 'Violence or dangerous acts' },
    { value: 'MISINFORMATION', label: 'Misinformation' },
    { value: 'COPYRIGHT', label: 'Copyright issue' },
    { value: 'OTHER', label: 'Other' },
];

//modal form allowing users to submit flags with specific violation reasons
const ReportModal = ({ reportableType, reportableId, onClose, onSuccess }) => {
    const [reason, setReason] = useState(REASON_LIST[0].value);
    const [details, setDetails] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (reason === 'OTHER' && !details.trim()) {
            setError('Details are required when "Other" is selected.');
            return;
        }

        try {
            setLoading(true);
            setError(null);
            await reportApi.createReport({
                reportableType,
                reportableId,
                reason,
                details
            });
            alert('Report submitted, our moderators will review it.');
            onSuccess();
        } catch (err) {
            if (err.response?.status === 409) {
                alert('You have already reported this content.');
                onSuccess();
            } else {
                setError(err.response?.data || 'Failed to submit report. Please try again.');
            }
        } finally {
            setLoading(false);
        }
    };

    return ReactDOM.createPortal(
        <div className="modal-overlay" onClick={(e) => { e.stopPropagation(); onClose(); }}>
            <div className="report-modal" onClick={(e) => e.stopPropagation()}>
                <button className="modal-close" onClick={onClose} aria-label="Close">
                    &times;
                </button>
                <div className="modal-header">
                    <h2>Report Content</h2>
                </div>
                
                {error && <div className="alert alert-error">{error}</div>}

                <form onSubmit={handleSubmit} className="modal-form">
                    <div className="form-group">
                        <label htmlFor="reason">Reason for reporting</label>
                        <select 
                            id="reason"
                            value={reason} 
                            onChange={(e) => setReason(e.target.value)}
                            className="form-control"
                            disabled={loading}
                        >
                            {REASON_LIST.map(r => (
                                <option key={r.value} value={r.value}>{r.label}</option>
                            ))}
                        </select>
                    </div>

                    {reason === 'OTHER' && (
                        <div className="form-group">
                            <label htmlFor="details">Details (required)</label>
                            <textarea
                                id="details"
                                value={details}
                                onChange={(e) => setDetails(e.target.value)}
                                className="form-control"
                                rows="4"
                                placeholder="Please provide more details about this report..."
                                required
                                disabled={loading}
                            />
                        </div>
                    )}

                    <div className="form-actions">
                        <button type="button" onClick={onClose} className="btn btn-secondary" disabled={loading}>
                            Cancel
                        </button>
                        <button type="submit" className="btn btn-primary" disabled={loading}>
                            {loading ? 'Submitting...' : 'Submit Report'}
                        </button>
                    </div>
                </form>
            </div>
        </div>,
        document.body
    );
};

export default ReportModal;
