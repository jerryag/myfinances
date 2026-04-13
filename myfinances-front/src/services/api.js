import axios from 'axios';

import { getValidM2MToken } from './m2mAuth';

const USER_KEY = 'user';
const USERS_ENDPOINT = '/users';

export const api = axios.create({
    baseURL: '/api'
});

api.interceptors.request.use(async config => {
    // 1. Autenticação Humana (Camada de Aplicação)
    const user = localStorage.getItem(USER_KEY);
    if (user) {
        const userData = JSON.parse(user);
        if (userData && userData.login) {
            config.headers['X-User-Login'] = userData.login;
        }
    }
    
    // 2. Autenticação M2M via Keycloak (Camada de Rede)
    const m2mToken = await getValidM2MToken();
    if (m2mToken) {
        config.headers.Authorization = `Bearer ${m2mToken}`;
    }

    return config;
});

export const userService = {
    getAll: (params) => api.get(USERS_ENDPOINT, { params }),
    create: (data) => api.post(USERS_ENDPOINT, data),
    update: (id, data) => api.put(`${USERS_ENDPOINT}/${id}`, data),
    changeStatus: (id, status) => api.patch(`${USERS_ENDPOINT}/${id}/status`, null, { params: { status } })
};
