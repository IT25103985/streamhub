import React, { useState, useEffect, useContext } from 'react';
import { Link } from 'react-router-dom';
import watchHistoryService from './watchHistoryService';
import { AuthContext } from '../../context/AuthContext';

//horizontal list of recommended videos based on watch history
const RecommendationsSection = () => {
    const { user } = useContext(AuthContext);
    const [recommendations, setRecommendations] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchRecommendations = async () => {
            if (!user) return;
            try {
                setLoading(true);
                const data = await watchHistoryService.getRecommendations(8);
                setRecommendations(data || []);
            } catch (err) {
                console.error("Failed to load recommendations:", err);
            } finally {
                setLoading(false);
            }
        };

        fetchRecommendations();
    }, [user]);

    if (!user || loading || recommendations.length === 0) {
        return null;
    }

    //recommendation video grid section
  return (
        <div className="my-8">
            <h2 className="text-2xl font-bold text-white mb-6 flex items-center gap-2">
                <svg className="w-6 h-6 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z" />
                </svg>
                Recommended for You
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
                {recommendations.map(video => (
                    <Link key={video.videoId} to={`/watch/${video.videoId}`} className="group flex flex-col gap-3 bg-white/5 p-3 rounded-2xl border border-white/5 hover:bg-white/10 transition-all duration-300">
                        <div className="aspect-video w-full bg-gray-800 rounded-xl overflow-hidden relative">
                            {video.thumbnailPath ? (
                                <img src={`http://localhost:8080${video.thumbnailPath}`} alt={video.title} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
                            ) : (
                                <div className="w-full h-full flex items-center justify-center text-gray-600 bg-gray-900 font-medium">No Thumbnail</div>
                            )}
                            <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                        </div>
                        <div>
                            <h3 className="text-white font-semibold line-clamp-2 leading-tight group-hover:text-blue-400 transition-colors">{video.title}</h3>
                            <p className="text-gray-400 text-sm mt-1">{video.ownerUsername}</p>
                            <p className="text-gray-500 text-xs mt-1">{video.viewCount || 0} views</p>
                        </div>
                    </Link>
                ))}
            </div>
        </div>
    );
};

export default RecommendationsSection;
