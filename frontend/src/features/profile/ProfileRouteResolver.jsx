import { useParams } from 'react-router-dom';
import { useContext } from 'react';
import { AuthContext } from '../../context/AuthContext';
import ProfilePage from './ProfilePage';
import PublicProfilePage from './PublicProfilePage';

//router helper resolving my profile vs public profile views
const ProfileRouteResolver = () => {
  const { userId } = useParams();
  const { user } = useContext(AuthContext);

  if (user && user.userId && userId && String(user.userId).toLowerCase() === String(userId).toLowerCase()) {
    return <ProfilePage />;
  }

  return <PublicProfilePage />;
};

export default ProfileRouteResolver;
