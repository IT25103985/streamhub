import React from 'react';
import { AdminTableSkeleton } from './skeletons/AdminTableRowSkeleton';

//reusable styled data table with sorting pagination and action menus
const AdminDataTable = ({ columns, data, actions, onRowClick, loading = false, loadingRows = 5 }) => {
  return (
    <div className="admin-data-table-container">
      <table className="admin-data-table">
        <thead>
          <tr>
            {columns.map((col, index) => (
              <th key={index}>{col.label}</th>
            ))}
            {actions && <th style={{ textAlign: 'right' }}>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <AdminTableSkeleton rows={loadingRows} columnsCount={columns.length} hasActions={!!actions} />
          ) : data && data.length > 0 ? (
            data.map((row, rowIndex) => (
              <tr 
                key={rowIndex}
                onClick={() => onRowClick && onRowClick(row)}
                style={onRowClick ? { cursor: 'pointer' } : {}}
                className={onRowClick ? 'clickable-row' : ''}
              >
                {columns.map((col, colIndex) => (
                  <td key={colIndex}>
                    {col.render ? col.render(row) : row[col.key]}
                  </td>
                ))}
                {actions && (
                  <td style={{ textAlign: 'right' }}>
                    <div className="admin-table-actions">
                      {actions(row)}
                    </div>
                  </td>
                )}
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan={columns.length + (actions ? 1 : 0)} style={{ textAlign: 'center', padding: '40px 20px', color: 'var(--text-faint)' }}>
                No records found.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
};

export default AdminDataTable;
