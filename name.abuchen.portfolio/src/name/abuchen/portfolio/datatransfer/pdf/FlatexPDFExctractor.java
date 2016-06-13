package name.abuchen.portfolio.datatransfer.pdf;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

public class FlatexPDFExctractor extends AbstractPDFExtractor
{

    public FlatexPDFExctractor(Client client) throws IOException
    {
        super(client);

        addBuySellTransaction();
        addBuyTransaction();
        addDepositAndWithdrawalTransaction();
        addDividendTransaction();
    }

    @SuppressWarnings("nls")
    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Sammelabrechnung (Wertpapierkauf/-verkauf)");
        this.addDocumentTyp(type);

        Block block = new Block("Nr.(\\d*)/(\\d*)  Kauf.*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        })

                        .section("wkn", "isin", "name")
                        .match("Nr.(\\d*)/(\\d*)  Kauf *(?<name>[^(]*) \\((?<isin>[^/]*)/(?<wkn>[^)]*)\\)")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares", "date")
                        .match("^davon ausgef\\. *: (?<shares>[.\\d]+,\\d*) St\\. *Schlusstag *: *(?<date>\\d+\\.\\d+\\.\\d{4}+), \\d+:\\d+ Uhr")
                        .assign((t, v) -> {
                            t.setShares(asShares(v.get("shares")));
                            t.setDate(asDate(v.get("date")));

                        })

                        .section("amount", "currency") //
                        .match(".* Endbetrag *: *(?<amount>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("fee", "currency").optional() //
                        .match(".* Provision *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* Eigene Spesen *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* \\*Fremde Spesen *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .wrap(t -> new BuySellEntryItem(t)));

        block = new Block("Nr.(\\d*)/(\\d*)  Verkauf.*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        })

                        .section("wkn", "isin", "name")
                        .match("Nr.(\\d*)/(\\d*)  Verkauf *(?<name>[^(]*) \\((?<isin>[^/]*)/(?<wkn>[^)]*)\\)")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares", "date")
                        .match("^davon ausgef\\. *: (?<shares>[.\\d]+,\\d*) St\\. *Schlusstag *: *(?<date>\\d+.\\d+.\\d{4}+), \\d+:\\d+ Uhr")
                        .assign((t, v) -> {
                            t.setShares(asShares(v.get("shares")));
                            t.setDate(asDate(v.get("date")));

                        })

                        .section("amount", "currency") //
                        .match(".* Endbetrag *: *(?<amount>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("fee", "currency").optional() //
                        .match(".* Provision *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* Eigene Spesen *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* \\*Fremde Spesen *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("tax", "currency").optional() //
                        .match(".* \\*\\*Einbeh. Steuer *: *(?<tax>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierabrechnung Kauf Fonds/Zertifikate");
        this.addDocumentTyp(type);

        Block block = new Block(" *biw AG *");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        })

                        .section("date").match(".*Schlusstag *(?<date>\\d+.\\d+.\\d{4}).*") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("wkn", "isin", "name")
                        .match("Nr.(\\d*)/(\\d*)  Kauf *(?<name>[^(]*) \\((?<isin>[^/]*)/(?<wkn>[^)]*)\\)") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares").match("^Ausgeführt *(?<shares>[\\.\\d]+(,\\d*)?) *St\\.") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency") //
                        .match(" * Endbetrag *(?<currency>\\w{3}+) *(?<amount>[\\d.-]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("fee", "currency").optional()
                        //
                        .match(".* Provision *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        //
                        .match(".* Eigene Spesen *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        //
                        .match(".* \\*Fremde Spesen *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addDepositAndWithdrawalTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug Nr:", (context, lines) -> {
            Pattern pYear = Pattern.compile("Kontoauszug Nr:[ ]*\\d+/(\\d+).*");
            Pattern pCurrency = Pattern.compile("Kontow.hrung:[ ]+(\\w{3}+)");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pYear.matcher(line);
                if (m.matches())
                {
                    context.put("year", m.group(1));
                }
                m = pCurrency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group(1));
                }
            }
        });
        this.addDocumentTyp(type);

        // deposit, add value to account
        // 01.01. 01.01. xyz 123,45+
        Block block = new Block("\\d+\\.\\d+\\.[ ]+\\d+\\.\\d+\\.[ ]+.berweisung[ ]+[\\d.-]+,\\d+[+-]");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.DEPOSIT);
            return t;
        })

        .section("valuta", "amount")
                        .match("\\d+.\\d+.[ ]+(?<valuta>\\d+.\\d+.)[ ]+.berweisung[ ]+(?<amount>[\\d.-]+,\\d+)(?<sign>[+-])")
                        .assign((t, v) -> {
                                Map<String, String> context = type.getCurrentContext();
                                String date = v.get("valuta");
                                if (date != null)
                                {
                                    // create a long date from the year in the
                                    // context
                                    t.setDate(asDate(date + context.get("year")));
                                }
                                t.setNote(v.get("text"));
                                t.setAmount(asAmount(v.get("amount")));
                                t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                                //check for withdrawals
                                String sign=v.get("sign");
                                if("-".equals(sign))
                                {
                                    //change type for withdrawals
                                    t.setType(AccountTransaction.Type.REMOVAL);
                                }
                        }).wrap(t -> new TransactionItem(t)));
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type1 = new DocumentType("Dividendengutschrift");
        DocumentType type2 = new DocumentType("Ertragsmitteilung");
        DocumentType type3 = new DocumentType("Zinsgutschrift");
        this.addDocumentTyp(type1);
        this.addDocumentTyp(type2);
        this.addDocumentTyp(type3);

        Block block = new Block("Ihre Depotnummer.*");
        type1.addBlock(block);
        type2.addBlock(block);
        type3.addBlock(block);
        block.set(new Transaction<AccountTransaction>()
                        //
                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DIVIDENDS);
                            return t;
                        })

                        .section("wkn", "isin", "name")
                        .match("Nr\\.(\\d*) * (?<name>[^(]*) *\\((?<isin>[^/]*)/(?<wkn>[^)]*)\\)").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares") //
                        .match("^St\\.[^:]+: *(?<shares>[\\.\\d]+(,\\d*)?).*")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency") //
                        .match(".* Endbetrag *: *(?<amount>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("date") //
                        .match("Valuta * : *(?<date>\\d+.\\d+.\\d{4}+).*")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .wrap(t -> new TransactionItem(t)));
    }

    @Override
    public String getLabel()
    {
        return "flatex"; //$NON-NLS-1$
    }
}