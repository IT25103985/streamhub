import React, { useState, useContext } from 'react';
import { AuthContext } from '../../context/AuthContext';
import ReportModal from './ReportModal';

//button triggering report modal for flagging inappropriate content
const ReportButton = ({ reportableType, reportableId, ownerId, className = '' }) => {
    const { user, openAuthModal } = useContext(AuthContext);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isReported, setIsReported] = useState(false);

    if (user && ownerId && user.userId === ownerId) {
        return null;
    }

    if (user && reportableType === 'PROFILE' && user.userId === reportableId) {
        return null;
    }

    const handleClick = () => {
        if (!user) {
            openAuthModal('login');
            return;
        }
        setIsModalOpen(true);
    };

    const handleSuccess = () => {
        setIsReported(true);
        setIsModalOpen(false);
    };

    return (
        <>
            <button
                className={`btn btn-secondary ${className}`}
                onClick={handleClick}
                disabled={isReported}
                title={isReported ? "Report submitted" : "Report this content"}
                style={{ padding: '6px 12px', fontSize: '13px', display: 'flex', alignItems: 'center', gap: '4px' }}
            >
                🚩 {isReported ? 'Reported' : 'Report'}
            </button>

            {isModalOpen && (
                <ReportModal
                    reportableType={reportableType}
                    reportableId={reportableId}
                    onClose={() => setIsModalOpen(false)}
                    onSuccess={handleSuccess}
                />
            )}
        </>
    );
};

export default ReportButton;
