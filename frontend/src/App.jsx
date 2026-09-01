import { BrowserRouter as Router, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { useContext } from 'react';
import { AuthContext } from './context/AuthContext';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import HomePage from './pages/HomePage';
import CategoryPage from './pages/CategoryPage';
import UploadVideoPage from './features/video/UploadVideoPage';
import CategoryManagement from './features/category/CategoryManagement';
import AdminVideosPage from './features/video/AdminVideosPage';
import AdminLayout from './features/admin/AdminLayout';
import AdminPlaylistsPage from './features/playlist/AdminPlaylistsPage';
import AdminCommentsPage from './features/social/AdminCommentsPage';
import AdminReportsPage from './features/report/AdminReportsPage';
import AuthModal from './components/AuthModal';
import InterestsModal from './features/onboarding/InterestsModal';
import WatchHistoryPage from './features/watchhistory/WatchHistoryPage';
import AdminAnalyticsPage from './features/admin/analytics/AdminAnalyticsPage';
import AdminUsersPage from './features/admin/users/AdminUsersPage';
import ProfilePage from './features/profile/ProfilePage';
import ProfileRouteResolver from './features/profile/ProfileRouteResolver';
import MyUploadsPage from './pages/MyUploadsPage';
import MyPlaylistsPage from './pages/MyPlaylistsPage';
import MyReportsPage from './pages/MyReportsPage';
import HelpPage from './pages/HelpPage';
import SettingsPage from './pages/SettingsPage';
import PlaylistWatchPage from './features/playlist/PlaylistWatchPage';
import SearchResultsPage from './pages/SearchResultsPage';
import ErrorBoundary from './components/ErrorBoundary';

//protect routes requiring specific roles
const AdminRoute = ({ user, allowedRoles, children }) => {
  if (!user || !allowedRoles.includes(user.role)) {
    return (
      <div className="admin-page" style={{ padding: '2rem', textAlign: 'center', color: '#ef4444' }}>
        <h2>403 - Forbidden</h2>
        <p>You do not have permission to view this page.</p>
      </div>
    );
  }
  return children;
};

//standard platform layout for public and user pages
const UserLayout = () => {
  return (
    <>
      <Navbar />
      <AuthModal />
      <Outlet />
      <Footer />
    </>
  );
};

function App() {
  const { user, loading, showInterestsModal, completeOnboarding } = useContext(AuthContext);

  if (loading) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', background: 'var(--void, #0a0d16)' }}>
        <div style={{ fontSize: '28px', fontWeight: 800, letterSpacing: '-0.02em', color: '#fff', marginBottom: '16px' }}>
          Stream<span style={{ color: '#e50914' }}>Hub</span>
        </div>
        <div className="skeleton-pulse" style={{ width: '48px', height: '4px', borderRadius: '2px', background: '#e50914' }} />
      </div>
    );
  }

  const isAdmin = user && (user.role === 'ADMINISTRATOR' || user.role === 'CONTENT_MODERATOR');

  return (
    <ErrorBoundary>
      <Router>
        {showInterestsModal && !isAdmin && <InterestsModal onSuccess={completeOnboarding} />}
        <Routes>
          {/* Main User and Public Routes */}
          <Route element={<UserLayout />}>
            <Route path="/" element={<HomePage />} />
            <Route path="/search" element={<SearchResultsPage />} />
            <Route path="/categories/:categoryId" element={<CategoryPage />} />
            <Route path="/upload" element={<UploadVideoPage />} />
            <Route path="/history" element={<WatchHistoryPage />} />
            <Route path="/watch-history" element={<WatchHistoryPage />} />
            <Route path="/profile" element={<ProfilePage />} />
            <Route path="/profile/:userId" element={<ProfileRouteResolver />} />
            <Route path="/my-uploads" element={<MyUploadsPage />} />
            <Route path="/my-playlists" element={<MyPlaylistsPage />} />
            <Route path="/my-reports" element={<MyReportsPage />} />
            <Route path="/help" element={<HelpPage />} />
            <Route path="/settings" element={<SettingsPage />} />
            <Route path="/playlist/:id" element={<PlaylistWatchPage />} />
          </Route>

          {/* Admin Dedicated Layout & Routes */}
          <Route path="/admin" element={
            <AdminRoute user={user} allowedRoles={['ADMINISTRATOR', 'CONTENT_MODERATOR']}>
              <AdminLayout />
            </AdminRoute>
          }>
            <Route index element={<Navigate to={user?.role === 'ADMINISTRATOR' ? "/admin/categories" : "/admin/videos"} replace />} />
            <Route path="categories" element={<AdminRoute user={user} allowedRoles={['ADMINISTRATOR']}><CategoryManagement /></AdminRoute>} />
            <Route path="users" element={<AdminRoute user={user} allowedRoles={['ADMINISTRATOR']}><AdminUsersPage /></AdminRoute>} />
            <Route path="videos" element={<AdminVideosPage />} />
            <Route path="playlists" element={<AdminPlaylistsPage />} />
            <Route path="comments" element={<AdminCommentsPage />} />
            <Route path="reports" element={<AdminReportsPage />} />
            <Route path="analytics" element={<AdminRoute user={user} allowedRoles={['ADMINISTRATOR']}><AdminAnalyticsPage /></AdminRoute>} />
            <Route path="*" element={<Navigate to={user?.role === 'ADMINISTRATOR' ? "/admin/categories" : "/admin/videos"} replace />} />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Router>
    </ErrorBoundary>
  );
}

export default App;

