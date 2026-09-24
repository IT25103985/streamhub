import React, { useState, useEffect, useRef, useContext } from 'react';
import axiosInstance from '../api/axiosInstance';
import { AuthContext } from '../context/AuthContext';
import Chip from './Chip';

//pill selector bar for filtering video feed by category
const CategoryChipBar = ({ selectedCategoryId, onCategoryChange }) => {
    const { user } = useContext(AuthContext);
    const [categories, setCategories] = useState([]);
    const [isLocked, setIsLocked] = useState(false);
    const barRef = useRef(null);

    useEffect(() => {
        axiosInstance.get('/categories')
            .then(res => setCategories(res.data))
            .catch(err => console.error('Error fetching categories', err));
    }, []);

    useEffect(() => {
        const handleScroll = () => {
            if (!barRef.current) return;
            const rect = barRef.current.getBoundingClientRect();
            setIsLocked(rect.top <= 51);
        };

        window.addEventListener('scroll', handleScroll, { passive: true });
        handleScroll();
        return () => window.removeEventListener('scroll', handleScroll);
    }, []);

    const currentFilter = selectedCategoryId || 'all';

    return (
        <div ref={barRef} className={`category-sticky-bar ${isLocked ? 'is-locked' : ''}`}>
            <div className="chip-row">
                <Chip 
                    active={currentFilter === 'all'} 
                    onClick={() => onCategoryChange(null)} 
                    filter="all" 
                    label="All" 
                />
                {user && (
                    <Chip 
                        active={currentFilter === 'following'} 
                        onClick={() => onCategoryChange('following')} 
                        filter="following" 
                        label="Following" 
                    />
                )}
                {categories.map(cat => (
                    <Chip 
                        key={cat.id} 
                        active={currentFilter === cat.id.toString()} 
                        onClick={() => onCategoryChange(cat.id.toString())} 
                        filter={cat.id.toString()} 
                        label={`${cat.icon || ''} ${cat.name}`.trim()} 
                    />
                ))}
            </div>
        </div>
    );
};

export default CategoryChipBar;
