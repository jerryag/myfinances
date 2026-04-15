import axios from 'axios';

import { getValidM2MToken } from './m2mAuth';

const USER_KEY = 'user';
const USERS_ENDPOINT = '/users';

export const api = axios.create({
    baseURL: '/api'
});

const SESSION_TOKEN_KEY = 'sessionToken';

api.interceptors.request.use(async config => {
    // 1. Autenticação Humana (Camada de Aplicação) via JWT de Sessão
    const sessionToken = localStorage.getItem(SESSION_TOKEN_KEY);
    if (sessionToken) {
        config.headers['X-Session-Token'] = sessionToken;
    }
    
    // 2. Autenticação M2M via Keycloak (Camada de Rede)
    const m2mToken = await getValidM2MToken();
    if (m2mToken) {
        config.headers.Authorization = `Bearer ${m2mToken}`;
    }

    return config;
});

api.interceptors.response.use(
    response => {
        const refreshedToken = response.headers['x-session-token'];
        if (refreshedToken) {
            localStorage.setItem(SESSION_TOKEN_KEY, refreshedToken);
        }
        return response;
    },
    error => {
        if (error.response && error.response.status === 401) {
            localStorage.removeItem(USER_KEY);
            localStorage.removeItem(SESSION_TOKEN_KEY);
            window.location.href = '/';
        }
        return Promise.reject(error);
    }
);

export const userService = {
    getAll: (params) => api.get(USERS_ENDPOINT, { params }),
    create: (data) => api.post(USERS_ENDPOINT, data),
    update: (id, data) => api.put(`${USERS_ENDPOINT}/${id}`, data),
    changeStatus: (id, status) => api.patch(`${USERS_ENDPOINT}/${id}/status`, null, { params: { status } })
};
