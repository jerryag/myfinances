import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

export const Login = () => {
    const [loginInput, setLoginInput] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const { login } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try {
            const user = await login(loginInput, password);
            if (user.changePwdOnLogin) {
                navigate('/change-password');
            } else {
                navigate('/home');
            }
        } catch (err) {
            console.error(err);
            setError(err.response?.data?.message || 'Erro ao realizar login');
        }
    };

    return (
        <div className="login-container">
            <div className="login-card">
                <h2>MyFinances</h2>
                {error && <div className="error-message">{error}</div>}
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label htmlFor="login">Login</label>
                        <input
                            type="email"
                            id="login"
                            name="username"
                            value={loginInput}
                            onChange={(e) => setLoginInput(e.target.value)}
                            placeholder="usuario@dominio.com"
                            required
                            autoComplete="username"
                            onInvalid={e => e.target.setCustomValidity(e.target.validity.valueMissing ? 'Por favor, informe seu login (e-mail).' : 'Por favor, inclua um "@" no endereço de e-mail.')}
                            onInput={e => e.target.setCustomValidity('')}
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="password">Senha</label>
                        <input
                            type="password"
                            id="password"
                            name="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="******"
                            required
                            autoComplete="current-password"
                            onInvalid={e => e.target.setCustomValidity('Por favor, informe sua senha.')}
                            onInput={e => e.target.setCustomValidity('')}
                        />
                    </div>
                    <button type="submit" className="btn-primary">Entrar</button>
                </form>
            </div>
        </div>
    );
};
