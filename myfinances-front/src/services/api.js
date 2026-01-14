import axios from 'axios';

export const api = axios.create({
    baseURL: '/api'
});

api.interceptors.request.use(async config => {
    const user = localStorage.getItem('user');
    if (user) {
        const userData = JSON.parse(user);
        config.headers['X-User-Login'] = userData.login;
    }
    return config;
});

export const userService = {
    getAll: (params) => api.get('/users', { params }),
    create: (data) => api.post('/users', data),
    update: (id, data) => api.put(`/users/${id}`, data),
    changeStatus: (id, status) => api.patch(`/users/${id}/status`, null, { params: { status } })
};
