export const getFullImageUrl = (url) => {
    if (!url) return null;
    if (url.startsWith('http')) return url;
    return `http://localhost:8081${url}`;
};
