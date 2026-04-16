import * as FaIcons from 'react-icons/fa';
import * as MdIcons from 'react-icons/md';
import * as IoIcons from 'react-icons/io5';
import * as SiIcons from 'react-icons/si';
import * as GiIcons from 'react-icons/gi';

// Define the static curated groups of icons
export const ICON_GROUPS = {
    'Alimentação': [
        'FaUtensils', 'FaHamburger', 'FaPizzaSlice', 'FaCoffee', 'FaShoppingBasket',
        'MdLocalDining', 'MdLocalGroceryStore', 'FaGlassCheers', 'MdCake', 'MdIcecream', 'FaFish',
        'FaBeer', 'FaWineGlass', 'SiIfood'
    ],
    'Contas Públicas': [
        'FaFileInvoiceDollar', 'FaCity', 'FaWifi', 'FaPhone', 'MdOutlineElectricalServices',
        'MdElectricCar', 'MdOutlinePhonelinkRing', 'FaHandHoldingWater', 'MdGasMeter'
    ],
    'Diversos': [
        'FaTag', 'FaQuestion', 'FaGift', 'FaBox', 'FaShoppingCart', 'FaCreditCard', 'FaUser',
        'FaPaw', 'FaSchool', 'FaBaby', 'FaGasPump', 'SiShopee', 'SiMercadopago', 'GiClothes',
        'FaGuitar'
    ],
    'Lazer': [
        'FaGamepad', 'FaFilm', 'FaTicketAlt', 'FaMusic', 'FaUmbrellaBeach',
        'FaFutbol', 'FaRunning', 'FaCampground', 'FaBowlingBall', 'FaChessKnight', 'FaTv', 'FaBeer'
    ],
    'Moradia': [
        'FaHome', 'FaBuilding', 'FaLightbulb', 'FaWater', 'FaCouch', 'FaBed', 'FaTools', 'MdHome', 'FaTrash'
    ],
    'Recebimentos': [
        'FaMoneyBillWave', 'FaHandHoldingUsd', 'FaPiggyBank', 'FaWallet', 'FaChartLine',
        'FaCoins', 'MdAttachMoney', 'MdTrendingUp'
    ],
    'Saúde e Beleza': [
        'FaHeartbeat', 'FaFirstAid', 'FaPills', 'FaUserMd', 'FaStethoscope', 'FaGlasses',
        'FaTooth', 'FaBrain', 'FaSyringe', 'FaHospital', 'MdHealthAndSafety', 'FaAirFreshener',
        'FaSpa', 'FaMagic', 'MdFace', 'MdFace2', 'IoDiamondSharp', 'GiLipstick', 'GiDelicatePerfume'
    ],
    'Serviços': [
        'FaAmazon', 'FaGoogle', 'FaAws', 'FaGithub', 'SiNetflix', 'FaYoutube', 'FaSpotify',
        'FaTools', 'FaOilCan', 'MdLocalLaundryService', 'GiGardeningShears', 'GiBroom',
        'IoBarChartOutline'
    ],
    'Transporte': [
        'FaCar', 'FaBus', 'FaTaxi', 'FaSubway', 'FaGasPump', 'FaUber',
        'FaBicycle', 'FaPlane', 'FaTrain', 'MdLocalGasStation', 'MdDirectionsCar', 'FaHelicopter', 'FaShip'
    ]
};

// Map of all available icons for rendering
const ALL_ICONS = {
    ...FaIcons,
    ...MdIcons,
    ...IoIcons,
    ...SiIcons,
    ...GiIcons
};

/**
 * Returns the React Component for a given icon name.
 * @param {string} iconName 
 * @returns {React.Component | null}
 */
export const getIcon = (iconName) => {
    if (!iconName) return null;
    return ALL_ICONS[iconName] || null;
};

/**
 * Returns the group name that contains the given icon name.
 * Useful for auto-selecting the group in dropdowns.
 * @param {string} iconName 
 * @returns {string | null}
 */
export const getGroupForIcon = (iconName) => {
    if (!iconName) return null;
    for (const [group, icons] of Object.entries(ICON_GROUPS)) {
        if (icons.includes(iconName)) {
            return group;
        }
    }
    return 'Diversos'; // Fallback
};
