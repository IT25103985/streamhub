import React from 'react';
import SkeletonBase from './SkeletonBase';
import './Skeleton.css';

//skeleton placeholder rows matching admindatatable column layout
export const AdminTableRowSkeleton = ({ columnsCount = 4, hasActions = true }) => {
  return (
    <tr className="admin-table-row-skeleton" aria-hidden="true">
      {Array.from({ length: columnsCount }).map((_, index) => (
        <td key={`skeleton-col-${index}`}>
          <SkeletonBase 
            width={index === 0 ? '75%' : index === 1 ? '60%' : '45%'} 
            height="14px" 
          />
        </td>
      ))}
      {hasActions && (
        <td style={{ textAlign: 'right' }}>
          <div style={{ display: 'inline-flex', gap: '8px', justifyContent: 'flex-end' }}>
            <SkeletonBase width="65px" height="28px" borderRadius="6px" />
          </div>
        </td>
      )}
    </tr>
  );
};

//renders multiple skeleton rows for table loading state
export const AdminTableSkeleton = ({ rows = 5, columnsCount = 4, hasActions = true }) => {
  return (
    <>
      {Array.from({ length: rows }).map((_, index) => (
        <AdminTableRowSkeleton 
          key={`admin-skeleton-row-${index}`} 
          columnsCount={columnsCount} 
          hasActions={hasActions} 
        />
      ))}
    </>
  );
};

export default AdminTableRowSkeleton;
