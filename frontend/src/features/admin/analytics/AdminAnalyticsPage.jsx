import React, { useState, useEffect } from 'react';
import watchHistoryService from '../../watchhistory/watchHistoryService';
import { Link } from 'react-router-dom';
import SkeletonBase from '../../../components/skeletons/SkeletonBase';

//dashboard showing platform aggregate statistics and user counts
const AdminAnalyticsPage = () => {
    const [trendingVideos, setTrendingVideos] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        //loads summary metrics from analytics endpoint
  const fetchAnalytics = async () => {
            try {
                setLoading(true);
                const data = await watchHistoryService.getAdminTrendingVideos(10);
                const filtered = (data || []).filter((v) => v && (v.viewCount || 0) >= 10);
                setTrendingVideos(filtered);
            } catch (err) {
                console.error("Failed to load analytics:", err);
                setError('Failed to load analytics data.');
            } finally {
                setLoading(false);
            }
        };

        fetchAnalytics();
    }, []);

    if (error) {
        return <div className="p-8 text-red-500">{error}</div>;
    }

    return (
        <div className="p-8">
            <h1 className="text-3xl font-bold text-white mb-2">Platform Analytics</h1>
            <p className="text-gray-400 mb-8">View platform-wide trending videos and aggregate statistics.</p>

            <div className="bg-white/5 border border-white/10 rounded-2xl p-6">
                <h2 className="text-xl font-bold text-white mb-6">Top Trending Videos</h2>
                
                {loading ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                        {Array.from({ length: 5 }).map((_, i) => (
                            <div key={`analytics-skel-${i}`} style={{ display: 'flex', alignItems: 'center', gap: '16px', padding: '12px 0', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                                <SkeletonBase width="30px" height="24px" />
                                <SkeletonBase width="64px" height="36px" borderRadius="4px" />
                                <SkeletonBase width="50%" height="20px" />
                                <SkeletonBase width="15%" height="20px" style={{ marginLeft: 'auto' }} />
                                <SkeletonBase width="50px" height="24px" borderRadius="12px" />
                            </div>
                        ))}
                    </div>
                ) : trendingVideos.length === 0 ? (
                    <div className="text-center py-8 text-gray-500">No viewing data available yet.</div>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="w-full text-left border-collapse">
                            <thead>
                                <tr className="border-b border-white/10">
                                    <th className="py-3 px-4 text-gray-400 font-medium">Rank</th>
                                    <th className="py-3 px-4 text-gray-400 font-medium">Video</th>
                                    <th className="py-3 px-4 text-gray-400 font-medium">Uploader</th>
                                    <th className="py-3 px-4 text-gray-400 font-medium text-right">Views</th>
                                </tr>
                            </thead>
                            <tbody>
                                {trendingVideos.map((video, index) => (
                                    <tr key={video.videoId} className="border-b border-white/5 hover:bg-white/5 transition-colors">
                                        <td className="py-3 px-4 text-white font-bold text-lg">#{index + 1}</td>
                                        <td className="py-3 px-4">
                                            <div className="flex items-center gap-3">
                                                <div className="w-16 h-9 bg-gray-800 rounded overflow-hidden flex-shrink-0">
                                                    {video.thumbnailPath ? (
                                                        <img src={`http://localhost:8080${video.thumbnailPath}`} alt={video.title} className="w-full h-full object-cover" />
                                                    ) : (
                                                        <div className="w-full h-full flex items-center justify-center text-[10px] text-gray-500">No Thumb</div>
                                                    )}
                                                </div>
                                                <Link to={`/watch/${video.videoId}`} className="text-white hover:text-blue-400 font-medium line-clamp-1">
                                                    {video.title}
                                                </Link>
                                            </div>
                                        </td>
                                        <td className="py-3 px-4 text-gray-300">{video.ownerUsername}</td>
                                        <td className="py-3 px-4 text-right">
                                            <span className="inline-flex items-center justify-center px-3 py-1 bg-blue-500/20 text-blue-400 rounded-full font-bold text-sm">
                                                {video.viewCount}
                                            </span>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
};

export default AdminAnalyticsPage;
