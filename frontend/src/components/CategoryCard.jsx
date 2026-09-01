import { Link } from 'react-router-dom';

//category card tile with gradient accents for browse page
const CategoryCard = ({ categoryId, slug, title, description, icon, bgUrl }) => {
  return (
    <Link 
      className={`card ${slug}`} 
      to={`/categories/${categoryId}`} 
      style={{ backgroundImage: `url('${bgUrl}')` }}
    >
      <span className="card-arrow">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2">
          <path d="M7 17 17 7M7 7h10v10" />
        </svg>
      </span>
      <div className="card-content">
        <span className="card-icon">{icon}</span>
        <h4>{title}</h4>
        <small>{description}</small>
      </div>
    </Link>
  );
};

export default CategoryCard;
