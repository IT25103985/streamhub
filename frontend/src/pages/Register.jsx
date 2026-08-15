import React, { useState, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

const Register = () => {
  const { register } = useContext(AuthContext);
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    username: '',
    firstName: '',
    lastName: '',
    email: '',
    dateOfBirth: '',
    password: '',
    confirmPassword: '',
    role: 'VIEWER'
  });

  const [error, setError] = useState(null);

  const passwordRules = {
    length: formData.password.length >= 8,
    uppercase: /[A-Z]/.test(formData.password),
    number: /[0-9]/.test(formData.password),
    special: /[@$!%*?&]/.test(formData.password)
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    try {
      await register(formData);
      navigate('/login');
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed');
    }
  };

  return (
    <div className="flex justify-center items-center min-h-[calc(100vh-64px)] bg-neutral-900 py-10">
      <div className="bg-neutral-800 p-8 rounded-lg shadow-lg w-full max-w-md border border-neutral-700">
        <h2 className="text-2xl font-bold text-white mb-6 text-center">Create an Account</h2>
        {error && <div className="bg-red-500/20 border border-red-500 text-red-400 p-3 rounded mb-4 text-sm">{error}</div>}
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-neutral-300 mb-1">First Name</label>
              <input type="text" name="firstName" value={formData.firstName} onChange={handleChange} required className="w-full bg-neutral-900 border border-neutral-700 text-white rounded p-2 focus:ring-2 focus:ring-primary-500 focus:outline-none" />
            </div>
            <div>
              <label className="block text-sm font-medium text-neutral-300 mb-1">Last Name</label>
              <input type="text" name="lastName" value={formData.lastName} onChange={handleChange} required className="w-full bg-neutral-900 border border-neutral-700 text-white rounded p-2 focus:ring-2 focus:ring-primary-500 focus:outline-none" />
            </div>
          </div>
          
          <div>
            <label className="block text-sm font-medium text-neutral-300 mb-1">Username</label>
            <input type="text" name="username" value={formData.username} onChange={handleChange} required className="w-full bg-neutral-900 border border-neutral-700 text-white rounded p-2 focus:ring-2 focus:ring-primary-500 focus:outline-none" />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-300 mb-1">Email</label>
            <input type="email" name="email" value={formData.email} onChange={handleChange} required className="w-full bg-neutral-900 border border-neutral-700 text-white rounded p-2 focus:ring-2 focus:ring-primary-500 focus:outline-none" />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-300 mb-1">Date of Birth</label>
            <input type="date" name="dateOfBirth" value={formData.dateOfBirth} onChange={handleChange} required className="w-full bg-neutral-900 border border-neutral-700 text-white rounded p-2 focus:ring-2 focus:ring-primary-500 focus:outline-none" />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-300 mb-1">Role</label>
            <select name="role" value={formData.role} onChange={handleChange} className="w-full bg-neutral-900 border border-neutral-700 text-white rounded p-2 focus:ring-2 focus:ring-primary-500 focus:outline-none">
              <option value="VIEWER">Viewer</option>
              <option value="CONTENT_CREATOR">Content Creator</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-300 mb-1">Password</label>
            <input type="password" name="password" value={formData.password} onChange={handleChange} required className="w-full bg-neutral-900 border border-neutral-700 text-white rounded p-2 focus:ring-2 focus:ring-primary-500 focus:outline-none" />
            
            <div className="mt-2 text-xs space-y-1">
              <div className={`flex items-center gap-2 ${passwordRules.length ? 'text-green-500' : 'text-neutral-500'}`}>
                <span className="w-4 h-4 inline-flex items-center justify-center border rounded-full">{passwordRules.length && '✓'}</span> At least 8 characters
              </div>
              <div className={`flex items-center gap-2 ${passwordRules.uppercase ? 'text-green-500' : 'text-neutral-500'}`}>
                <span className="w-4 h-4 inline-flex items-center justify-center border rounded-full">{passwordRules.uppercase && '✓'}</span> One uppercase letter
              </div>
              <div className={`flex items-center gap-2 ${passwordRules.number ? 'text-green-500' : 'text-neutral-500'}`}>
                <span className="w-4 h-4 inline-flex items-center justify-center border rounded-full">{passwordRules.number && '✓'}</span> One number
              </div>
              <div className={`flex items-center gap-2 ${passwordRules.special ? 'text-green-500' : 'text-neutral-500'}`}>
                <span className="w-4 h-4 inline-flex items-center justify-center border rounded-full">{passwordRules.special && '✓'}</span> One special character
              </div>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-300 mb-1">Confirm Password</label>
            <input type="password" name="confirmPassword" value={formData.confirmPassword} onChange={handleChange} required className="w-full bg-neutral-900 border border-neutral-700 text-white rounded p-2 focus:ring-2 focus:ring-primary-500 focus:outline-none" />
          </div>

          <button type="submit" className="w-full bg-primary-600 hover:bg-primary-500 text-white font-medium py-2 rounded transition-colors mt-6">
            Register
          </button>
        </form>
      </div>
    </div>
  );
};

export default Register;
