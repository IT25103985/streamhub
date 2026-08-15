import { Link } from 'react-router-dom';

const Footer = () => {
  return (
    <footer>
      <div className="footer-inner">
        <Link to="/" className="logo">Stream<span>Hub</span></Link>
        <p>Online video browsing &amp; management system.</p>
      </div>
    </footer>
  );
};

export default Footer;
