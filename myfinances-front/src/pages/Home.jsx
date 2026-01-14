import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { usePageTitle } from '../context/PageTitleContext';

export const Home = () => {
    const { user } = useAuth();
    const navigate = useNavigate();
    usePageTitle('Home');

    return (
        <div className="home-container">
            <main className="dashboard-grid">
                {['ADMIN', 'MASTER'].includes(user.type) && (
                    <div className="card">
                        <h3>Usuários</h3>
                        <p>Gerencie usuários e acessos.</p>
                        <button onClick={() => navigate('/users')} className="btn-card">Acessar</button>
                    </div>
                )}

                <div className="card">
                    <h3>Tipos de Transação</h3>
                    <p>Cadastre tipos de receitas e despesas.</p>
                    <button onClick={() => navigate('/transaction-types')} className="btn-card">Acessar</button>
                </div>

                <div className="card">
                    <h3>Lançamentos</h3>
                    <p>Gerencie suas receitas e despesas.</p>
                    <button className="btn-card">Acessar</button>
                </div>
                <div className="card">
                    <h3>Relatórios</h3>
                    <p>Visualize seus gastos mensais.</p>
                    <button className="btn-card">Acessar</button>
                </div>
                <div className="card">
                    <h3>Perfil</h3>
                    <p>Atualize seus dados cadastrais.</p>
                    <button className="btn-card">Acessar</button>
                </div>
                <div className="card">
                    <h3>Configurações</h3>
                    <p>Preferências do sistema.</p>
                    <button className="btn-card">Acessar</button>
                </div>
            </main >
        </div >
    );
};
