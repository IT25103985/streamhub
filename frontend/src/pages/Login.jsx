import React, { useState, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';

const Login = () => {
  const { login } = useContext(AuthContext);
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    email: '',
    password: '',
    keepMeLoggedIn: false
  });

  const [error, setError] = useState(null);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData({
      ...formData,
      [name]: type === 'checkbox' ? checked : value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    try {
      await login(formData.email, formData.password, formData.keepMeLoggedIn);
      navigate('/'); // redirect to home on success
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed. Check your credentials.');
    }
  };

  return (
    <div className="flex justify-center items-center min-h-[calc(100vh-64px)] bg-neutral-900 py-10">
      <div className="bg-neutral-800 p-8 rounded-lg shadow-lg w-full max-w-sm border border-neutral-700">
        <h2 className="text-2xl font-bold text-white mb-6 text-center">Welcome Back</h2>
        {error && <div className="bg-red-500/20 border border-red-500 text-red-400 p-3 rounded mb-4 text-sm">{error}</div>}
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-neutral-300 mb-1">Email</label>
            <input type="email" name="email" value={formData.email} onChange={handleChange} required className="w-full bg-neutral-900 border border-neutral-700 text-white rounded p-2 focus:ring-2 focus:ring-primary-500 focus:outline-none" />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-300 mb-1">Password</label>
            <input type="password" name="password" value={formData.password} onChange={handleChange} required className="w-full bg-neutral-900 border border-neutral-700 text-white rounded p-2 focus:ring-2 focus:ring-primary-500 focus:outline-none" />
          </div>

          <div className="flex items-center gap-2 mt-2">
            <input 
              type="checkbox" 
              id="keepMeLoggedIn"
              name="keepMeLoggedIn"
              checked={formData.keepMeLoggedIn}
              onChange={handleChange}
              className="rounded bg-neutral-900 border-neutral-700 text-primary-500 focus:ring-primary-500"
            />
            <label htmlFor="keepMeLoggedIn" className="text-sm text-neutral-300">Keep me logged in</label>
          </div>

          <button type="submit" className="w-full bg-primary-600 hover:bg-primary-500 text-white font-medium py-2 rounded transition-colors mt-6">
            Log In
          </button>
        </form>
        
        <div className="mt-4 text-center text-sm text-neutral-400">
          Don't have an account? <Link to="/register" className="text-primary-400 hover:text-primary-300">Register here</Link>
        </div>
      </div>
    </div>
  );
};

export default Login;
