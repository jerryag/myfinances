import axios from 'axios';
import { jwtDecode } from 'jwt-decode';

const M2M_TOKEN_KEY = 'm2m_token';

let isRefreshing = false;
let refreshSubscribers = [];

const subscribeTokenRefresh = (cb) => {
    refreshSubscribers.push(cb);
};

const onTokenRefreshed = (token) => {
    refreshSubscribers.forEach((cb) => cb(token));
    refreshSubscribers = [];
};

export const getValidM2MToken = async () => {
    let token = localStorage.getItem(M2M_TOKEN_KEY);

    if (token) {
        try {
            const decoded = jwtDecode(token);
            const now = Math.floor(Date.now() / 1000);
            
            // Se faltar mais de 30 segundos, usa ele mesmo
            if (decoded.exp - now > 30) {
                return token;
            }
        } catch (e) {
            // Token de formato invalido
        }
    }

    if (isRefreshing) {
        return new Promise((resolve) => {
            subscribeTokenRefresh((newToken) => {
                resolve(newToken);
            });
        });
    }

    isRefreshing = true;

    try {
        const params = new URLSearchParams();
        params.append('grant_type', 'client_credentials');
        params.append('client_id', import.meta.env.VITE_OAUTH2_CLIENT_ID);
        params.append('client_secret', import.meta.env.VITE_OAUTH2_CLIENT_SECRET);
        
        if (import.meta.env.VITE_OAUTH2_SCOPE) {
            params.append('scope', import.meta.env.VITE_OAUTH2_SCOPE);
        }

        const response = await axios.post(
            import.meta.env.VITE_OAUTH2_TOKEN_URI,
            params,
            { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
        );

        token = response.data.access_token;
        localStorage.setItem(M2M_TOKEN_KEY, token);
        onTokenRefreshed(token);
    } catch (err) {
        console.error("Falha ao reverter token M2M:", err);
        // Em caso de falha drástica de credenciais do Client
        token = null;
        onTokenRefreshed(null);
    } finally {
        isRefreshing = false;
    }

    return token;
};
