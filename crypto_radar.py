import psycopg
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from datetime import datetime
import warnings
warnings.filterwarnings('ignore')

# Database configuration
DB_CONFIG = {
    'host': 'localhost',
    'dbname': 'your_database_name',
    'user': 'your_username',
    'password': 'your_password',
    'port': 5432
}

class EthereumFraudAnalyzer:
    def __init__(self, db_config, sample_size=500):
        """Initialize the analyzer with database connection and sample size."""
        self.db_config = db_config
        self.sample_size = sample_size
        self.conn = None
        self.sampled_addresses = []
        
    def connect_db(self):
        """Establish database connection."""
        try:
            # Create connection string for psycopg3
            conn_string = f"host={self.db_config['host']} port={self.db_config['port']} dbname={self.db_config['dbname']} user={self.db_config['user']} password={self.db_config['password']}"
            self.conn = psycopg.connect(conn_string)
            print("✓ Database connection established")
            return True
        except Exception as e:
            print(f"✗ Database connection failed: {e}")
            return False
    
    def close_db(self):
        """Close database connection."""
        if self.conn:
            self.conn.close()
            print("✓ Database connection closed")
    
    def sample_addresses(self):
        """Randomly sample addresses from the database."""
        query = """
        SELECT from_address 
        FROM (
            SELECT DISTINCT from_address 
            FROM transaction_native_transfers
        ) AS distinct_addresses
        ORDER BY RANDOM() 
        LIMIT %s
        """
        try:
            df = pd.read_sql_query(query, self.conn, params=(self.sample_size,))
            self.sampled_addresses = df['from_address'].tolist()
            print(f"✓ Sampled {len(self.sampled_addresses)} addresses")
            return self.sampled_addresses
        except Exception as e:
            print(f"✗ Address sampling failed: {e}")
            return []
    
    def fetch_transaction_data(self):
        """Fetch comprehensive transaction data for analysis."""
        query = """
        SELECT 
            tnt.from_address,
            tnt.to_address,
            tnt.value,
            tnt.gas_price,
            tnt.gas_limit,
            tnt.nonce,
            tnt.type,
            tnt.token,
            tnt.is_contract_interaction,
            tnt.gas_z_score,
            tnt.value_z_score,
            b.timestamp,
            b.block_number,
            b.base_fee_per_gas
        FROM transaction_native_transfers tnt
        JOIN blocks b ON tnt.block_block_number = b.block_number
        ORDER BY b.timestamp
        """
        try:
            df = pd.read_sql_query(query, self.conn)
            df['timestamp'] = pd.to_datetime(df['timestamp'])
            df['value_eth'] = df['value'].astype(float) / 1e18
            df['is_sampled'] = df['from_address'].isin(self.sampled_addresses)
            print(f"✓ Fetched {len(df)} transactions")
            return df
        except Exception as e:
            print(f"✗ Data fetch failed: {e}")
            return pd.DataFrame()
    
    def fetch_erc20_data(self):
        """Fetch ERC20 transfer data."""
        query = """
        SELECT 
            erc.from_address,
            erc.to_address,
            erc.value,
            erc.token,
            erc.type,
            b.timestamp,
            b.block_number
        FROM transaction_erc20_transfers erc
        JOIN blocks b ON erc.block_block_number = b.block_number
        ORDER BY b.timestamp
        """
        try:
            df = pd.read_sql_query(query, self.conn)
            df['timestamp'] = pd.to_datetime(df['timestamp'])
            df['is_sampled'] = df['from_address'].isin(self.sampled_addresses)
            print(f"✓ Fetched {len(df)} ERC20 transfers")
            return df
        except Exception as e:
            print(f"✗ ERC20 data fetch failed: {e}")
            return pd.DataFrame()
    
    def analyze_transaction_frequency(self, df):
        """Analyze transaction frequency over time."""
        df['date'] = df['timestamp'].dt.date
        
        # Group by date and sampled status
        freq_sampled = df[df['is_sampled']].groupby('date').size()
        freq_others = df[~df['is_sampled']].groupby('date').size()
        
        # Normalize by number of addresses
        freq_sampled_norm = freq_sampled / len(self.sampled_addresses)
        freq_others_norm = freq_others / (df[~df['is_sampled']]['from_address'].nunique())
        
        return freq_sampled_norm, freq_others_norm
    
    def analyze_value_distribution(self, df):
        """Analyze transaction value distributions."""
        sampled_values = df[df['is_sampled']]['value_eth']
        other_values = df[~df['is_sampled']]['value_eth']
        
        return sampled_values, other_values
    
    def analyze_hourly_patterns(self, df):
        """Analyze transaction patterns by hour of day."""
        df['hour'] = df['timestamp'].dt.hour
        
        hourly_sampled = df[df['is_sampled']].groupby('hour').size()
        hourly_others = df[~df['is_sampled']].groupby('hour').size()
        
        # Normalize
        hourly_sampled = hourly_sampled / len(self.sampled_addresses)
        hourly_others = hourly_others / df[~df['is_sampled']]['from_address'].nunique()
        
        return hourly_sampled, hourly_others
    
    def analyze_gas_behavior(self, df):
        """Analyze gas usage patterns."""
        sampled_gas = df[df['is_sampled']]['gas_price'].astype(float) / 1e9  # Convert to Gwei
        other_gas = df[~df['is_sampled']]['gas_price'].astype(float) / 1e9
        
        return sampled_gas, other_gas
    
    def analyze_per_block_activity(self, df):
        """Analyze activity per block."""
        sampled_blocks = df[df['is_sampled']].groupby('block_number').agg({
            'from_address': 'count',
            'value_eth': 'sum'
        }).rename(columns={'from_address': 'tx_count', 'value_eth': 'total_value'})
        
        other_blocks = df[~df['is_sampled']].groupby('block_number').agg({
            'from_address': 'count',
            'value_eth': 'sum'
        }).rename(columns={'from_address': 'tx_count', 'value_eth': 'total_value'})
        
        return sampled_blocks, other_blocks
    
    def plot_all_statistics(self, df, erc20_df=None):
        """Generate comprehensive visualization dashboard."""
        fig = plt.figure(figsize=(20, 12))
        
        # 1. Transaction Frequency Over Time
        ax1 = plt.subplot(3, 3, 1)
        freq_sampled, freq_others = self.analyze_transaction_frequency(df)
        ax1.plot(freq_sampled.index, freq_sampled.values, label='Sampled (500)', linewidth=2, alpha=0.7)
        ax1.plot(freq_others.index, freq_others.values, label='Others', linewidth=2, alpha=0.7)
        ax1.set_xlabel('Date')
        ax1.set_ylabel('Avg Transactions per Address')
        ax1.set_title('Transaction Frequency Over Time')
        ax1.legend()
        ax1.grid(True, alpha=0.3)
        plt.setp(ax1.xaxis.get_majorticklabels(), rotation=45)
        
        # 2. Value Distribution (Log Scale)
        ax2 = plt.subplot(3, 3, 2)
        sampled_vals, other_vals = self.analyze_value_distribution(df)
        sampled_vals_filtered = sampled_vals[sampled_vals > 0]
        other_vals_filtered = other_vals[other_vals > 0]
        ax2.hist(np.log10(sampled_vals_filtered + 1e-10), bins=50, alpha=0.6, label='Sampled', density=True)
        ax2.hist(np.log10(other_vals_filtered + 1e-10), bins=50, alpha=0.6, label='Others', density=True)
        ax2.set_xlabel('Log10(ETH Value)')
        ax2.set_ylabel('Density')
        ax2.set_title('Transaction Value Distribution')
        ax2.legend()
        ax2.grid(True, alpha=0.3)
        
        # 3. Hourly Activity Pattern
        ax3 = plt.subplot(3, 3, 3)
        hourly_sampled, hourly_others = self.analyze_hourly_patterns(df)
        hours = range(24)
        ax3.plot(hours, [hourly_sampled.get(h, 0) for h in hours], 'o-', label='Sampled', linewidth=2)
        ax3.plot(hours, [hourly_others.get(h, 0) for h in hours], 's-', label='Others', linewidth=2)
        ax3.set_xlabel('Hour of Day')
        ax3.set_ylabel('Avg Transactions per Address')
        ax3.set_title('Hourly Transaction Pattern')
        ax3.set_xticks(range(0, 24, 3))
        ax3.legend()
        ax3.grid(True, alpha=0.3)
        
        # 4. Gas Price Distribution
        ax4 = plt.subplot(3, 3, 4)
        sampled_gas, other_gas = self.analyze_gas_behavior(df)
        sampled_gas_filtered = sampled_gas[sampled_gas > 0]
        other_gas_filtered = other_gas[other_gas > 0]
        ax4.boxplot([sampled_gas_filtered, other_gas_filtered], labels=['Sampled', 'Others'])
        ax4.set_ylabel('Gas Price (Gwei)')
        ax4.set_title('Gas Price Distribution')
        ax4.set_yscale('log')
        ax4.grid(True, alpha=0.3)
        
        # 5. Cumulative Transaction Count
        ax5 = plt.subplot(3, 3, 5)
        sampled_cumsum = df[df['is_sampled']].groupby('date')['from_address'].count().cumsum()
        others_cumsum = df[~df['is_sampled']].groupby('date')['from_address'].count().cumsum()
        ax5.plot(sampled_cumsum.index, sampled_cumsum.values, label='Sampled', linewidth=2)
        ax5.plot(others_cumsum.index, others_cumsum.values, label='Others', linewidth=2)
        ax5.set_xlabel('Date')
        ax5.set_ylabel('Cumulative Transactions')
        ax5.set_title('Cumulative Transaction Count')
        ax5.legend()
        ax5.grid(True, alpha=0.3)
        plt.setp(ax5.xaxis.get_majorticklabels(), rotation=45)
        
        # 6. Transactions per Block
        ax6 = plt.subplot(3, 3, 6)
        sampled_blocks, other_blocks = self.analyze_per_block_activity(df)
        ax6.scatter(sampled_blocks.index, sampled_blocks['tx_count'], alpha=0.5, s=10, label='Sampled')
        ax6.scatter(other_blocks.index, other_blocks['tx_count'], alpha=0.5, s=10, label='Others')
        ax6.set_xlabel('Block Number')
        ax6.set_ylabel('Transactions per Block')
        ax6.set_title('Transaction Count per Block')
        ax6.legend()
        ax6.grid(True, alpha=0.3)
        
        # 7. Value per Block
        ax7 = plt.subplot(3, 3, 7)
        ax7.scatter(sampled_blocks.index, sampled_blocks['total_value'], alpha=0.5, s=10, label='Sampled')
        ax7.scatter(other_blocks.index, other_blocks['total_value'], alpha=0.5, s=10, label='Others')
        ax7.set_xlabel('Block Number')
        ax7.set_ylabel('Total ETH Value per Block')
        ax7.set_title('Value Transferred per Block')
        ax7.set_yscale('log')
        ax7.legend()
        ax7.grid(True, alpha=0.3)
        
        # 8. Z-Score Distribution (Anomaly Detection)
        ax8 = plt.subplot(3, 3, 8)
        sampled_z = df[df['is_sampled']]['value_z_score'].dropna()
        other_z = df[~df['is_sampled']]['value_z_score'].dropna()
        ax8.hist(sampled_z, bins=50, alpha=0.6, label='Sampled', range=(-5, 5), density=True)
        ax8.hist(other_z, bins=50, alpha=0.6, label='Others', range=(-5, 5), density=True)
        ax8.set_xlabel('Value Z-Score')
        ax8.set_ylabel('Density')
        ax8.set_title('Transaction Value Anomaly Score')
        ax8.axvline(x=3, color='r', linestyle='--', alpha=0.5, label='Anomaly Threshold')
        ax8.axvline(x=-3, color='r', linestyle='--', alpha=0.5)
        ax8.legend()
        ax8.grid(True, alpha=0.3)
        
        # 9. Contract Interaction Ratio
        ax9 = plt.subplot(3, 3, 9)
        sampled_contract = df[df['is_sampled']]['is_contract_interaction'].value_counts(normalize=True)
        other_contract = df[~df['is_sampled']]['is_contract_interaction'].value_counts(normalize=True)
        x = np.arange(2)
        width = 0.35
        ax9.bar(x - width/2, [sampled_contract.get(True, 0), sampled_contract.get(False, 0)], 
                width, label='Sampled', alpha=0.8)
        ax9.bar(x + width/2, [other_contract.get(True, 0), other_contract.get(False, 0)], 
                width, label='Others', alpha=0.8)
        ax9.set_ylabel('Proportion')
        ax9.set_title('Contract Interaction Ratio')
        ax9.set_xticks(x)
        ax9.set_xticklabels(['Contract', 'EOA'])
        ax9.legend()
        ax9.grid(True, alpha=0.3, axis='y')
        
        plt.tight_layout()
        plt.savefig('fraud_detection_analysis.png', dpi=300, bbox_inches='tight')
        print("✓ Comprehensive analysis plot saved as 'fraud_detection_analysis.png'")
        plt.show()
    
    def generate_summary_statistics(self, df):
        """Generate summary statistics comparing sampled vs others."""
        print("\n" + "="*80)
        print("SUMMARY STATISTICS")
        print("="*80)
        
        sampled = df[df['is_sampled']]
        others = df[~df['is_sampled']]
        
        stats = {
            'Metric': [],
            'Sampled (500)': [],
            'Others': [],
            'Ratio (S/O)': []
        }
        
        # Total transactions
        stats['Metric'].append('Total Transactions')
        stats['Sampled (500)'].append(f"{len(sampled):,}")
        stats['Others'].append(f"{len(others):,}")
        stats['Ratio (S/O)'].append(f"{len(sampled)/len(others):.4f}")
        
        # Avg transactions per address
        stats['Metric'].append('Avg Tx per Address')
        avg_sampled = len(sampled) / len(self.sampled_addresses)
        avg_others = len(others) / others['from_address'].nunique()
        stats['Sampled (500)'].append(f"{avg_sampled:.2f}")
        stats['Others'].append(f"{avg_others:.2f}")
        stats['Ratio (S/O)'].append(f"{avg_sampled/avg_others:.4f}")
        
        # Total value transferred
        stats['Metric'].append('Total ETH Transferred')
        stats['Sampled (500)'].append(f"{sampled['value_eth'].sum():.2f}")
        stats['Others'].append(f"{others['value_eth'].sum():.2f}")
        stats['Ratio (S/O)'].append(f"{sampled['value_eth'].sum()/others['value_eth'].sum():.4f}")
        
        # Average transaction value
        stats['Metric'].append('Avg Tx Value (ETH)')
        stats['Sampled (500)'].append(f"{sampled['value_eth'].mean():.6f}")
        stats['Others'].append(f"{others['value_eth'].mean():.6f}")
        stats['Ratio (S/O)'].append(f"{sampled['value_eth'].mean()/others['value_eth'].mean():.4f}")
        
        # Median transaction value
        stats['Metric'].append('Median Tx Value (ETH)')
        stats['Sampled (500)'].append(f"{sampled['value_eth'].median():.6f}")
        stats['Others'].append(f"{others['value_eth'].median():.6f}")
        ratio = sampled['value_eth'].median()/others['value_eth'].median() if others['value_eth'].median() > 0 else 0
        stats['Ratio (S/O)'].append(f"{ratio:.4f}")
        
        # Average gas price
        stats['Metric'].append('Avg Gas Price (Gwei)')
        sampled_gas_avg = sampled['gas_price'].astype(float).mean() / 1e9
        others_gas_avg = others['gas_price'].astype(float).mean() / 1e9
        stats['Sampled (500)'].append(f"{sampled_gas_avg:.2f}")
        stats['Others'].append(f"{others_gas_avg:.2f}")
        stats['Ratio (S/O)'].append(f"{sampled_gas_avg/others_gas_avg:.4f}")
        
        # Contract interaction rate
        stats['Metric'].append('Contract Interaction %')
        sampled_contract_pct = sampled['is_contract_interaction'].mean() * 100
        others_contract_pct = others['is_contract_interaction'].mean() * 100
        stats['Sampled (500)'].append(f"{sampled_contract_pct:.2f}%")
        stats['Others'].append(f"{others_contract_pct:.2f}%")
        stats['Ratio (S/O)'].append(f"{sampled_contract_pct/others_contract_pct:.4f}")
        
        # Anomaly rate (|z-score| > 3)
        sampled_anomaly = (sampled['value_z_score'].abs() > 3).mean() * 100
        others_anomaly = (others['value_z_score'].abs() > 3).mean() * 100
        stats['Metric'].append('Anomaly Rate %')
        stats['Sampled (500)'].append(f"{sampled_anomaly:.2f}%")
        stats['Others'].append(f"{others_anomaly:.2f}%")
        ratio = sampled_anomaly/others_anomaly if others_anomaly > 0 else 0
        stats['Ratio (S/O)'].append(f"{ratio:.4f}")
        
        stats_df = pd.DataFrame(stats)
        print(stats_df.to_string(index=False))
        print("="*80 + "\n")
        
        return stats_df
    
    def run_analysis(self):
        """Execute complete analysis pipeline."""
        print("\n" + "="*80)
        print("ETHEREUM FRAUD DETECTION ANALYSIS")
        print("="*80 + "\n")
        
        if not self.connect_db():
            return
        
        try:
            # Sample addresses
            self.sample_addresses()
            
            # Fetch data
            print("\nFetching transaction data...")
            df = self.fetch_transaction_data()
            
            if df.empty:
                print("No data available for analysis")
                return
            
            # Optional: Fetch ERC20 data
            print("\nFetching ERC20 transfer data...")
            erc20_df = self.fetch_erc20_data()
            
            # Generate statistics
            self.generate_summary_statistics(df)
            
            # Generate visualizations
            print("\nGenerating visualizations...")
            self.plot_all_statistics(df, erc20_df)
            
            print("\n✓ Analysis complete!")
            
        except Exception as e:
            print(f"\n✗ Analysis failed: {e}")
            import traceback
            traceback.print_exc()
        finally:
            self.close_db()


if __name__ == "__main__":
    # Configure your database connection
    DB_CONFIG = {
        'host': 'localhost',
        'dbname': 'cryptoradar',     # Update with your database name
        'user': 'admin',          # Update with your username
        'password': 'admin',      # Update with your password
        'port': 5858
    }
    
    # Initialize and run analyzer
    analyzer = EthereumFraudAnalyzer(DB_CONFIG, sample_size=500)
    analyzer.run_analysis()