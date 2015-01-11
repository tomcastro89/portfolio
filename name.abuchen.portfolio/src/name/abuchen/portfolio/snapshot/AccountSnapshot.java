package name.abuchen.portfolio.snapshot;

import java.util.Date;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.Money;

public class AccountSnapshot
{
    // //////////////////////////////////////////////////////////////
    // factory methods
    // //////////////////////////////////////////////////////////////

    @Deprecated
    public static AccountSnapshot create(Account account, Date time)
    {
        CurrencyConverter converter = new CurrencyConverterImpl(null, account.getCurrencyCode());
        return create(account, converter, time);
    }

    public static AccountSnapshot create(Account account, CurrencyConverter converter, Date date)
    {
        long funds = 0;

        for (AccountTransaction t : account.getTransactions())
        {
            if (t.getDate().getTime() <= date.getTime())
            {
                switch (t.getType())
                {
                    case DEPOSIT:
                    case DIVIDENDS:
                    case INTEREST:
                    case SELL:
                    case TRANSFER_IN:
                    case TAX_REFUND:
                        funds += t.getAmount();
                        break;
                    case FEES:
                    case TAXES:
                    case REMOVAL:
                    case BUY:
                    case TRANSFER_OUT:
                        funds -= t.getAmount();
                        break;
                    default:
                        throw new RuntimeException("Unknown Account Transaction type: " + t.getType()); //$NON-NLS-1$
                }
            }
        }

        return new AccountSnapshot(account, date, converter, Money.of(account.getCurrencyCode(), funds));
    }

    // //////////////////////////////////////////////////////////////
    // instance impl
    // //////////////////////////////////////////////////////////////

    private final Account account;
    private final Date date;
    private final CurrencyConverter converter;
    private final Money funds;

    private AccountSnapshot(Account account, Date date, CurrencyConverter converter, Money funds)
    {
        this.account = account;
        this.date = date;
        this.converter = converter;
        this.funds = funds;
    }

    public Account getAccount()
    {
        return account;
    }

    public Date getTime()
    {
        return date;
    }

    public CurrencyConverter getCurrencyConverter()
    {
        return converter;
    }

    public Money getFunds()
    {
        return converter.convert(date, funds);
    }

    public Money getUnconvertedFunds()
    {
        return funds;
    }
}