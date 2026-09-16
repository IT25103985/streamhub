import { Link, useNavigate } from 'react-router-dom';

//page footer with platform branding and navigation links
const Footer = () => {
  const navigate = useNavigate();

  const handleLogoClick = (e) => {
    e.preventDefault();
    window.dispatchEvent(new CustomEvent('streamhub-reset-home'));
    window.scrollTo({ top: 0, behavior: 'smooth' });
    navigate('/');
  };

  return (
    <footer>
      <div className="footer-inner">
        <Link to="/" onClick={handleLogoClick} className="logo">Stream<span>Hub</span></Link>
        <p>Online video browsing &amp; management system.</p>
      </div>
    </footer>
  );
};

export default Footer;
