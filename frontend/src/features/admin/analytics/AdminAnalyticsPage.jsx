import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid } from 'recharts';
import adminAnalyticsService from './adminAnalyticsService';
import SkeletonBase from '../../../components/skeletons/SkeletonBase';

//dashboard showing platform summary statistics signup trends and trending content
const AdminAnalyticsPage = () => {
    const [analytics, setAnalytics] = useState(null);
    const [trendingVideos, setTrendingVideos] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const fetchAnalyticsData = async () => {
        try {
            setLoading(true);
            setError(null);
            
            //loads aggregate metrics and trending list concurrently
            const [analyticsRes, trendingRes] = await Promise.allSettled([
                adminAnalyticsService.getAnalytics(),
                adminAnalyticsService.getTrendingVideos(10)
            ]);

            if (analyticsRes.status === 'fulfilled') {
                setAnalytics(analyticsRes.value);
            } else {
                const errMsg = analyticsRes.reason?.response?.data?.message || analyticsRes.reason?.message || 'Failed to load analytics data.';
                console.error("Failed to load analytics:", analyticsRes.reason);
                setError(errMsg);
            }

            if (trendingRes.status === 'fulfilled') {
                const filtered = (trendingRes.value || []).filter((v) => v && (v.viewCount || 0) >= 10);
                setTrendingVideos(filtered);
            }
        } catch (err) {
            console.error("Failed to load analytics:", err);
            setError(err?.response?.data?.message || 'Failed to load analytics data.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchAnalyticsData();
    }, []);

    const statCards = [
        { label: 'Total Users', value: analytics?.totalUsers ?? 0 },
        { label: 'Total Videos', value: analytics?.totalVideos ?? 0 },
        { label: 'Total Categories', value: analytics?.totalCategories ?? 0 },
        { label: 'Pending Reports', value: analytics?.pendingReports ?? 0 },
        { label: 'Total Comments & Likes', value: analytics?.totalCommentsAndLikes ?? 0 }
    ];

    if (error && !analytics) {
        return (
            <div className="p-8">
                <h1 className="text-3xl font-bold text-white mb-2">Platform Analytics</h1>
                <p className="text-gray-400 mb-8">View platform-wide statistics and activity trends.</p>
                <div style={{ background: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.2)', color: '#f87171', padding: '20px', borderRadius: 'var(--radius-md, 16px)' }}>
                    <p style={{ fontWeight: 600, marginBottom: '8px' }}>Failed to load analytics data</p>
                    <p style={{ fontSize: '14px', color: 'var(--text-dim, #9aa3c0)', marginBottom: '16px' }}>{error}</p>
                    <button
                        onClick={fetchAnalyticsData}
                        style={{
                            background: 'var(--accent, #5B5FEF)',
                            color: '#fff',
                            border: 'none',
                            padding: '8px 16px',
                            borderRadius: 'var(--radius-sm, 10px)',
                            cursor: 'pointer',
                            fontSize: '14px',
                            fontWeight: 600
                        }}
                    >
                        Retry
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="p-8">
            <h1 className="text-3xl font-bold text-white mb-2">Platform Analytics</h1>
            <p className="text-gray-400 mb-8">View platform-wide statistics and activity trends.</p>

            {/* Stat Cards Grid */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginBottom: '24px' }}>
                {statCards.map((card, idx) => (
                    <div
                        key={idx}
                        style={{
                            background: 'var(--surface-2, #252530)',
                            border: '1px solid var(--border, rgba(255,255,255,.08))',
                            borderRadius: 'var(--radius-md, 16px)',
                            padding: '20px'
                        }}
                    >
                        <div style={{ fontSize: '13px', color: 'var(--text-dim, #9aa3c0)', marginBottom: '8px', fontWeight: 500 }}>
                            {card.label}
                        </div>
                        {loading ? (
                            <SkeletonBase width="60px" height="32px" borderRadius="6px" />
                        ) : (
                            <div style={{ fontSize: '28px', fontWeight: 700, color: 'var(--text, #F5F5F7)', letterSpacing: '-0.02em' }}>
                                {card.value.toLocaleString()}
                            </div>
                        )}
                    </div>
                ))}
            </div>

            {/* Signups Chart */}
            <div
                style={{
                    background: 'var(--surface-2, #252530)',
                    border: '1px solid var(--border, rgba(255,255,255,.08))',
                    borderRadius: 'var(--radius-md, 16px)',
                    padding: '24px',
                    marginBottom: '24px'
                }}
            >
                <h2 style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text, #F5F5F7)', marginBottom: '16px' }}>
                    Signups (Last 7 Days)
                </h2>
                {loading ? (
                    <div style={{ height: '240px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <SkeletonBase width="100%" height="240px" borderRadius="8px" />
                    </div>
                ) : (
                    <div style={{ width: '100%', height: 260 }}>
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart data={analytics?.signupsLast7Days || []} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                                <CartesianGrid stroke="rgba(255,255,255,0.06)" strokeDasharray="3 3" vertical={false} />
                                <XAxis
                                    dataKey="date"
                                    stroke="var(--text-dim, #9aa3c0)"
                                    fontSize={12}
                                    tickLine={false}
                                    axisLine={{ stroke: 'var(--border, rgba(255,255,255,.08))' }}
                                />
                                <YAxis
                                    stroke="var(--text-dim, #9aa3c0)"
                                    fontSize={12}
                                    allowDecimals={false}
                                    tickLine={false}
                                    axisLine={{ stroke: 'var(--border, rgba(255,255,255,.08))' }}
                                />
                                <Tooltip
                                    cursor={{ fill: 'rgba(255,255,255,0.04)' }}
                                    contentStyle={{
                                        background: 'var(--surface, #1B1B22)',
                                        border: '1px solid var(--border, rgba(255,255,255,.08))',
                                        borderRadius: 'var(--radius-sm, 10px)',
                                        color: 'var(--text, #F5F5F7)',
                                        fontSize: '13px'
                                    }}
                                />
                                <Bar dataKey="count" name="New Signups" fill="var(--accent, #5B5FEF)" radius={[6, 6, 0, 0]} />
                            </BarChart>
                        </ResponsiveContainer>
                    </div>
                )}
            </div>

            {/* Trending Videos Table */}
            <div
                style={{
                    background: 'var(--surface-2, #252530)',
                    border: '1px solid var(--border, rgba(255,255,255,.08))',
                    borderRadius: 'var(--radius-md, 16px)',
                    padding: '24px'
                }}
            >
                <h2 style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text, #F5F5F7)', marginBottom: '16px' }}>
                    Top Trending Videos
                </h2>
                
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
                                                        <img src={`http://localhost:8081${video.thumbnailPath}`} alt={video.title} className="w-full h-full object-cover" />
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
