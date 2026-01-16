# CryptoRadar - Sistem Mimarisi

## Genel Bakış

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                    CRYPTORADAR SYSTEM                                    │
└─────────────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────┐     HTTP/REST      ┌──────────────────────────────────────────────────┐
│                  │◄──────────────────►│                                                  │
│    FRONTEND      │                    │              BACKEND (Spring Boot)               │
│   (React + Vite) │                    │                                                  │
│                  │                    │  ┌─────────────┐  ┌─────────────┐  ┌──────────┐  │
│  ┌────────────┐  │                    │  │ Controllers │  │  Services   │  │   JPA    │  │
│  │ Dashboard  │  │    Port: 5173      │  │             │  │             │  │ Repos    │  │
│  ├────────────┤  │◄──────────────────►│  │ - Watchlist │  │ - Alchemy   │  │          │  │
│  │Transactions│  │                    │  │ - Fraud     │  │ - Fraud     │  │          │  │
│  ├────────────┤  │                    │  │ - Dashboard │  │ - RiskScore │  │          │  │
│  │  Wallets   │  │                    │  │ - History   │  │ - Watchlist │  │          │  │
│  ├────────────┤  │                    │  └──────┬──────┘  └──────┬──────┘  └────┬─────┘  │
│  │  History   │  │                    │         │                │              │        │
│  └────────────┘  │                    │         └────────────────┴──────────────┘        │
│                  │                    │                          │                       │
│  Recharts        │                    │                    Port: 8080                    │
│  Tailwind CSS    │                    │                                                  │
│  Lucide Icons    │                    └──────────────────────────┬───────────────────────┘
└──────────────────┘                                               │
                                                                   │
                    ┌──────────────────────────────────────────────┼───────────────────────┐
                    │                                              │                       │
                    ▼                                              ▼                       ▼
        ┌──────────────────────┐              ┌──────────────────────────┐    ┌────────────────────┐
        │   ML SERVICE         │              │      PostgreSQL DB       │    │   Alchemy API      │
        │   (Python FastAPI)   │              │                          │    │   (External)       │
        │                      │              │  ┌────────────────────┐  │    │                    │
        │  ┌────────────────┐  │              │  │ address_features   │  │    │  Ethereum          │
        │  │ XGBoost Model  │  │              │  ├────────────────────┤  │    │  Blockchain        │
        │  │                │  │              │  │ fraud_check_history│  │    │  Data              │
        │  │ - Trained on   │  │              │  ├────────────────────┤  │    │                    │
        │  │   Kaggle Data  │  │              │  │ watchlist_txs      │  │    │  - Transfers       │
        │  │ - 17 Features  │  │              │  ├────────────────────┤  │    │  - Balances        │
        │  │ - Binary Class │  │              │  │ blocks             │  │    │  - Token Info      │
        │  └────────────────┘  │              │  ├────────────────────┤  │    │                    │
        │                      │              │  │ native_transfers   │  │    │                    │
        │  Port: 7900          │              │  └────────────────────┘  │    │  api.alchemy.com   │
        │  /predict endpoint   │              │                          │    │                    │
        └──────────────────────┘              │  Port: 5432              │    └────────────────────┘
                                              └──────────────────────────┘
```

## Veri Akış Diyagramı

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                           FRAUD DETECTION FLOW                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘

    ┌──────────┐                                                              
    │  User    │                                                              
    │  Input   │  1. Enter Ethereum Address                                   
    └────┬─────┘                                                              
         │                                                                    
         ▼                                                                    
┌─────────────────┐                                                           
│    FRONTEND     │  2. POST /api/watchlist/add                               
│    (React)      │─────────────────────────────────────┐                     
└─────────────────┘                                     │                     
                                                        ▼                     
                                              ┌─────────────────────┐         
                                              │  AddressWatchlist   │         
                                              │     Service         │         
                                              └──────────┬──────────┘         
                                                         │                    
                           3. Fetch blockchain data      │                    
                              from Alchemy API           │                    
                                                         ▼                    
                                              ┌─────────────────────┐         
                                              │   Alchemy Service   │◄────────┐
                                              │                     │         │
                                              │ - getOutgoingTx()   │    ┌────┴────┐
                                              │ - getIncomingTx()   │    │ Alchemy │
                                              │ - getBalance()      │    │   API   │
                                              └──────────┬──────────┘    └─────────┘
                                                         │                    
                           4. Calculate features         │                    
                              (17 metrics)               │                    
                                                         ▼                    
                                              ┌─────────────────────┐         
                                              │  Feature Extraction │         
                                              │                     │         
                                              │ - sentTnx           │         
                                              │ - receivedTnx       │         
                                              │ - totalEtherSent    │         
                                              │ - avgValSent        │         
                                              │ - uniqueAddresses   │         
                                              │ - timeDiff          │         
                                              │ - ...               │         
                                              └──────────┬──────────┘         
                                                         │                    
                           5. Save to database           │                    
                                                         ▼                    
                                              ┌─────────────────────┐         
                                              │    PostgreSQL       │         
                                              │  address_features   │         
                                              └──────────┬──────────┘         
                                                         │                    
         ┌───────────────────────────────────────────────┘                    
         │                                                                    
         │  6. User clicks "Check Fraud"                                      
         │     POST /api/fraud/check                                          
         ▼                                                                    
┌─────────────────┐                                                           
│ FraudDetection  │  7. Load features from DB                                 
│    Service      │                                                           
└────────┬────────┘                                                           
         │                                                                    
         │  8. Send features to ML model                                      
         │     POST http://localhost:7900/predict                             
         ▼                                                                    
┌─────────────────┐                                                           
│   ML SERVICE    │  9. XGBoost prediction                                    
│   (FastAPI)     │                                                           
│                 │     Input: 17 features                                    
│  ┌───────────┐  │     Output: {                                             
│  │  XGBoost  │  │       "fraud": true/false,                                
│  │   Model   │  │       "fraudProbability": 0.0-1.0,                        
│  └───────────┘  │       "normalProbability": 0.0-1.0                        
│                 │     }                                                     
└────────┬────────┘                                                           
         │                                                                    
         │  10. Return prediction                                             
         ▼                                                                    
┌─────────────────┐                                                           
│  RiskScoring    │  11. Calculate risk score (0-100)                         
│    Service      │      based on:                                            
│                 │      - ML probability (50%)                               
│                 │      - Transaction patterns (15%)                         
│                 │      - Value analysis (15%)                               
│                 │      - Timing patterns (10%)                              
│                 │      - Network analysis (10%)                             
└────────┬────────┘                                                           
         │                                                                    
         │  12. Save to fraud_check_history                                   
         │                                                                    
         ▼                                                                    
┌─────────────────┐                                                           
│    Response     │  13. Return to frontend                                   
│                 │                                                           
│  {              │      {                                                    
│    address,     │        "fraud": true,                                     
│    fraud,       │        "fraudProbability": 0.714,                         
│    riskScore,   │        "riskScore": 62,                                   
│    riskLevel,   │        "riskLevel": "MEDIUM",                             
│    riskFactors  │        "riskFactors": [                                   
│  }              │          "High fraud probability (71.4%)",                
│                 │          "Mostly sends transactions"                      
└────────┬────────┘        ]                                                  
         │              }                                                     
         ▼                                                                    
┌─────────────────┐                                                           
│    FRONTEND     │  14. Display results                                      
│   Risk Gauge    │      - Risk score gauge                                   
│   Risk Factors  │      - Risk level badge                                   
│   Probability   │      - Risk factors list                                  
└─────────────────┘      - Probability bars                                   
```

## Teknoloji Stack

### Frontend
| Teknoloji | Versiyon | Kullanım |
|-----------|----------|----------|
| React | 19.x | UI Framework |
| Vite | 7.x | Build Tool |
| Tailwind CSS | 4.x | Styling |
| Recharts | 3.x | Charts & Graphs |
| React Router | 6.x | Navigation |
| Lucide React | - | Icons |

### Backend
| Teknoloji | Versiyon | Kullanım |
|-----------|----------|----------|
| Java | 17+ | Runtime |
| Spring Boot | 3.x | Framework |
| Spring Data JPA | - | ORM |
| PostgreSQL | 15+ | Database |
| Lombok | - | Boilerplate reduction |

### ML Service
| Teknoloji | Versiyon | Kullanım |
|-----------|----------|----------|
| Python | 3.10+ | Runtime |
| FastAPI | - | REST API |
| XGBoost | - | ML Model |
| Pandas | - | Data Processing |
| Scikit-learn | - | ML Utilities |

### External Services
| Servis | Kullanım |
|--------|----------|
| Alchemy API | Ethereum blockchain data |
| Etherscan | Address verification links |

## Database Schema

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              DATABASE SCHEMA                                 │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────┐       ┌─────────────────────────┐
│   address_features      │       │   fraud_check_history   │
├─────────────────────────┤       ├─────────────────────────┤
│ PK address VARCHAR(42)  │       │ PK id BIGINT            │
├─────────────────────────┤       ├─────────────────────────┤
│ sent_tnx BIGINT         │       │ address VARCHAR(42)     │
│ received_tnx BIGINT     │       │ checked_at TIMESTAMP    │
│ total_transactions      │       │ is_fraud BOOLEAN        │
│ total_ether_sent        │       │ fraud_probability DEC   │
│ total_ether_received    │       │ normal_probability DEC  │
│ total_ether_balance     │       │ risk_score INTEGER      │
│ min_val_sent            │       │ risk_factors TEXT       │
│ max_val_sent            │       │ message VARCHAR         │
│ avg_val_sent            │       └─────────────────────────┘
│ min_value_received      │
│ max_value_received      │       ┌─────────────────────────┐
│ avg_val_received        │       │   watchlist_txs         │
│ unique_sent_to          │       ├─────────────────────────┤
│ unique_received_from    │       │ PK id BIGINT            │
│ time_diff_first_last    │       ├─────────────────────────┤
│ avg_min_between_sent    │       │ hash VARCHAR            │
│ avg_min_between_recv    │       │ watched_address         │
│ num_created_contracts   │       │ from_address            │
│ total_ether_contracts   │       │ to_address              │
│ total_erc20_tnxs        │       │ value DECIMAL           │
│ first_tx_timestamp      │       │ asset VARCHAR           │
│ last_tx_timestamp       │       │ category VARCHAR        │
│ created_at TIMESTAMP    │       │ block_timestamp         │
│ last_updated TIMESTAMP  │       │ is_outgoing BOOLEAN     │
└─────────────────────────┘       └─────────────────────────┘
```

## Port Yapılandırması

| Servis | Port | URL |
|--------|------|-----|
| Frontend (Vite) | 5173 | http://localhost:5173 |
| Backend (Spring) | 8080 | http://localhost:8080 |
| ML Service (FastAPI) | 7900 | http://localhost:7900 |
| PostgreSQL | 5432 | localhost:5432 |

## API Endpoints

### Watchlist API
```
GET    /api/watchlist              - Tüm izlenen adresleri getir
GET    /api/watchlist/{address}    - Tek adres detayı
POST   /api/watchlist/add          - Yeni adres ekle
DELETE /api/watchlist/remove       - Adres sil
```

### Fraud API
```
GET    /api/fraud/check            - Fraud kontrolü yap
```

### Fraud History API
```
GET    /api/fraud-history          - Tüm geçmiş (paginated)
GET    /api/fraud-history/address/{addr} - Adres geçmişi
GET    /api/fraud-history/fraudulent     - Sadece fraud olanlar
GET    /api/fraud-history/stats          - İstatistikler
```

### Dashboard API
```
GET    /api/dashboard/stats                    - Genel istatistikler
GET    /api/dashboard/transactions/recent      - Son işlemler
GET    /api/dashboard/transactions/by-address  - Adrese göre işlemler
GET    /api/dashboard/addresses/top            - En aktif adresler
```

### ML Service API
```
POST   /predict                    - Fraud tahmini yap
```
