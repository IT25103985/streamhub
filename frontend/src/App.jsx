import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import HomePage from './pages/HomePage';
import CategoryPage from './pages/CategoryPage';
import UploadVideoPage from './features/video/UploadVideoPage';

function App() {
  return (
    <Router>
      <Navbar />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/categories/:categoryId" element={<CategoryPage />} />
        <Route path="/upload" element={<UploadVideoPage />} />
      </Routes>
      <Footer />
    </Router>
  );
}

export default App;
