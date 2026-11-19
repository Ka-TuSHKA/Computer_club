import { useState, useEffect } from 'react';
import { fetchProfile } from '../api/userApi';

export const useProfile = (token) => {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadProfile = () => {
    if (!token) return;
    setLoading(true);
    fetchProfile(token)
      .then(data => { setProfile(data); setLoading(false); })
      .catch(err => { setError(err.message); setLoading(false); });
  };

  useEffect(() => {
    loadProfile();
  }, [token]);

  return { profile, loading, error, loadProfile };
};
