package com.maxcape.accounthistory;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class AccountHistoryPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(AccountHistoryPlugin.class);
		RuneLite.main(args);
	}
}
