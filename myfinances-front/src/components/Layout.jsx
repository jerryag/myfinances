import { Outlet } from 'react-router-dom';
import { Header } from './Header';
import { PageTitleProvider } from '../context/PageTitleContext';

export const Layout = () => {
    return (
        <PageTitleProvider>
            <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
                <Header />
                <main style={{ flex: 1, padding: '0 2rem' }}>
                    <Outlet />
                </main>
            </div>
        </PageTitleProvider>
    );
};
