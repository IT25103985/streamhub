//interactive pill chip button for selection filters
const Chip = ({ filter, active, onClick, label, count }) => {
  return (
    <button 
      className={`chip ${active ? 'active' : ''}`} 
      onClick={() => onClick(filter)}
    >
      {label}
      {count !== undefined && <span>{count}</span>}
    </button>
  );
};

export default Chip;
