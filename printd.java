// Identify parent package.
package core;
 
// Import libraries.
import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;
 
import java.net.*;
import java.io.*;
import java.util.*;
 
 
// Encapsulating class.
public class printd {
 
	private static Scanner scanner;
 
	// Initializes the account.
	private static void fundAccount(String accountID) throws MalformedURLException, IOException {
		boolean verbose = false;
		String friendbotUrl = String.format("https://horizon-testnet.stellar.org/friendbot?addr=%s",accountID);
		InputStream response = new URL(friendbotUrl).openStream();
		scanner = new Scanner(response, "UTF-8");
		String body = scanner.useDelimiter("\\A").next();
		System.out.println("SUCCESS! You have a new account :)\n");
		if (verbose == true) {
			System.out.println(body);
		}
	}
 
	// Takes: (String secretKeyOfSender, String publicAccountIdOfRecipient)
	private static void sendTokens(String fromSecret, String toAccountId) throws IOException {
		Network.useTestNetwork();
		Server server = new Server("https://horizon-testnet.stellar.org");
 
		KeyPair source = KeyPair.fromSecretSeed(fromSecret);
		KeyPair destination = KeyPair.fromAccountId(toAccountId);
 
		// First, check to make sure that the destination account exists.
		// You could skip this, but if the account does not exist, you will be charged
		// the transaction fee when the transaction fails.
		// It will throw HttpResponseException if account does not exist or there was another error.
		server.accounts().account(destination);
 
		// If there was no error, load up-to-date information on your account.
		AccountResponse sourceAccount = server.accounts().account(source);
 
		// Start building the transaction.
		Transaction transaction = new Transaction.Builder(sourceAccount)
				.addOperation(new PaymentOperation.Builder(destination, new AssetTypeNative(), "10").build())
		        // A memo allows you to add your own metadata to a transaction. It's
		        // optional and does not affect how Stellar treats the transaction.
		        .addMemo(Memo.text("Test Transaction"))
		        .build();
		// Sign the transaction to prove you are actually the person sending it.
		transaction.sign(source);
 
		// And finally, send it off to Stellar!
		try {
		  SubmitTransactionResponse response = server.submitTransaction(transaction);
		  System.out.println("Success!");
		  System.out.println(response);
		} catch (Exception e) {
		  System.out.println("Something went wrong!");
		  System.out.println(e.getMessage());
		  // If the result is unknown (no response body, timeout etc.) we simply resubmit
		  // already built transaction:
		  // SubmitTransactionResponse response = server.submitTransaction(transaction);
		}
	}
 
	// Returns balance of the account given.
	private static int getBalance(KeyPair paird) throws IOException {
		Server server = new Server("https://horizon-testnet.stellar.org");
		AccountResponse account = server.accounts().account(paird);
		System.out.println("Balances for account " + paird.getAccountId());
		String balanced = null;
		for (AccountResponse.Balance balance : account.getBalances()) {
		  //System.out.println(String.format("Type: %s, Code: %s, Balance: %s",balance.getAssetType(),balance.getAssetCode(),balance.getBalance()));
		  balanced = balance.getBalance();
		}
		return Integer.parseInt(balanced);
	}
 
	private static void issueCurrency(KeyPair issuerPair, KeyPair recieverPair) throws IOException {
		Network.useTestNetwork();
		Server server = new Server("https://horizon-testnet.stellar.org");
 
		// Keys for accounts to issue and receive the new asset
		KeyPair issuingKeys = issuerPair;
		KeyPair receivingKeys = recieverPair;
 
		// Create an object to represent the new asset
		Asset WickNote = Asset.createNonNativeAsset("WIC", issuingKeys);
 
		// First, the receiving account must trust the asset
		AccountResponse receiving = server.accounts().account(receivingKeys);
		Transaction allowWickNotes = new Transaction.Builder(receiving)
		  .addOperation(
		    // The `ChangeTrust` operation creates (or alters) a trustline
		    // The second parameter limits the amount the account can hold
		    new ChangeTrustOperation.Builder(WickNote, "100000000").build())
		  .build();
		allowWickNotes.sign(receivingKeys);
		server.submitTransaction(allowWickNotes);
		System.out.println("Server authenticated trust transaction between " + issuerPair.getAccountId().toString() + " and " + recieverPair.getAccountId());
 
		// Second, the issuing account actually sends a payment using the asset
		String amount = "100000";
		AccountResponse issuing = server.accounts().account(issuingKeys);
		Transaction sendWickNotes = new Transaction.Builder(issuing)
		  .addOperation(
		    new PaymentOperation.Builder(receivingKeys, WickNote, amount).build())
		  .build();
		sendWickNotes.sign(issuingKeys);
		server.submitTransaction(sendWickNotes);
		System.out.println("Server sent " + amount + " WickNote (WIC) from " + issuerPair.getAccountId().toString() + " to " + recieverPair.getAccountId());
		 
	}
 
	// Runs stuff.
	private static KeyPair server() throws MalformedURLException, IOException {
		KeyPair paird = printd.newKey();
		System.out.println("Created pair.\nPublic: " + paird.getAccountId().toString() + "\nPrivate: " + String.valueOf(paird.getSecretSeed()) + "\n");
		printd.fundAccount(paird.getAccountId());
		return paird;
	}
 
	// Generates new pair.
	private static KeyPair newKey() {
		// create a completely new and unique pair of keys.
		// see more about KeyPair objects: https://stellar.github.io/java-stellar-sdk/org/stellar/sdk/KeyPair.html
		KeyPair pair = KeyPair.random();
		return pair;
	}
 
	// Runs the server.
	public static void main (String args[]) throws MalformedURLException, IOException {
		KeyPair pair = server();
		KeyPair pair2 = server();
		issueCurrency(pair,pair2);
	}
}