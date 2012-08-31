/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2012 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package imagej.plugin;

import imagej.Contextual;
import imagej.InstantiableException;
import imagej.Prioritized;
import imagej.command.Command;
import imagej.command.CommandInfo;
import imagej.command.CommandModule;
import imagej.log.LogService;
import imagej.module.Module;
import imagej.module.ModuleInfo;
import imagej.module.ModuleService;
import imagej.service.AbstractService;
import imagej.service.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Default service for keeping track of available plugins. Available plugins are
 * discovered using a library called SezPoz. Loading of the actual plugin
 * classes can be deferred until a particular plugin's first execution.
 * 
 * @author Curtis Rueden
 * @see IPlugin
 * @see Plugin
 */
@Plugin(type = Service.class)
public class DefaultPluginService extends AbstractService implements
	PluginService
{

	@Parameter
	private LogService log;

	@Parameter
	private ModuleService moduleService;

	/** Index of registered plugins. */
	private PluginIndex pluginIndex;

	// -- PluginService methods --

	@Override
	public ModuleService getModuleService() {
		return moduleService;
	}

	@Override
	public PluginIndex getIndex() {
		return pluginIndex;
	}

	@Override
	public void reloadPlugins() {
		// remove old commands from module service
		moduleService.removeModules(getCommands());

		pluginIndex.clear();
		pluginIndex.discover();

		// add new commands to module service
		moduleService.addModules(getCommands());
	}

	@Override
	public void addPlugin(final PluginInfo<?> plugin) {
		pluginIndex.add(plugin);
		if (plugin instanceof ModuleInfo) {
			moduleService.addModule((ModuleInfo) plugin);
		}
	}

	@Override
	public <T extends PluginInfo<?>> void
		addPlugins(final Collection<T> plugins)
	{
		pluginIndex.addAll(plugins);

		// add new commands to module service
		final List<ModuleInfo> modules = new ArrayList<ModuleInfo>();
		for (final PluginInfo<?> info : plugins) {
			if (info instanceof ModuleInfo) {
				modules.add((ModuleInfo) info);
			}
		}
		moduleService.addModules(modules);
	}

	@Override
	public void removePlugin(final PluginInfo<?> plugin) {
		pluginIndex.remove(plugin);
		if (plugin instanceof ModuleInfo) {
			moduleService.removeModule((ModuleInfo) plugin);
		}
	}

	@Override
	public <T extends PluginInfo<?>> void removePlugins(
		final Collection<T> plugins)
	{
		pluginIndex.removeAll(plugins);

		// remove old commands to module service
		final List<ModuleInfo> modules = new ArrayList<ModuleInfo>();
		for (final PluginInfo<?> info : plugins) {
			if (info instanceof ModuleInfo) {
				modules.add((ModuleInfo) info);
			}
		}
		moduleService.removeModules(modules);
	}

	@Override
	public List<PluginInfo<?>> getPlugins() {
		return pluginIndex.getAll();
	}

	@Override
	public <P extends IPlugin> PluginInfo<P>
		getPlugin(final Class<P> pluginClass)
	{
		return first(getPluginsOfClass(pluginClass));
	}

	@Override
	public PluginInfo<IPlugin> getPlugin(final String className) {
		return first(getPluginsOfClass(className));
	}

	@Override
	public <P extends IPlugin> List<PluginInfo<? extends P>> getPluginsOfType(
		final Class<P> type)
	{
		return pluginIndex.getPlugins(type);
	}

	@Override
	public <P extends IPlugin> List<PluginInfo<P>> getPluginsOfClass(
		final Class<P> pluginClass)
	{
		final ArrayList<PluginInfo<P>> result = new ArrayList<PluginInfo<P>>();
		getPluginsOfClass(pluginClass.getName(), getPlugins(), result);
		return result;
	}

	@Override
	public List<PluginInfo<IPlugin>> getPluginsOfClass(final String className) {
		final ArrayList<PluginInfo<IPlugin>> result =
			new ArrayList<PluginInfo<IPlugin>>();
		getPluginsOfClass(className, getPlugins(), result);
		return result;
	}

	@Override
	public List<CommandInfo<Command>> getCommands() {
		return getCommandsOfType(Command.class);
	}

	@Override
	public <C extends Command> CommandInfo<C> getCommand(
		final Class<C> commandClass)
	{
		return first(getCommandsOfClass(commandClass));
	}

	@Override
	public CommandInfo<Command> getCommand(
		final String className)
	{
		return first(getCommandsOfClass(className));
	}

	@Override
	public <C extends Command> List<CommandInfo<C>>
		getCommandsOfType(final Class<C> type)
	{
		final List<PluginInfo<?>> list = pluginIndex.get(type);
		@SuppressWarnings({ "rawtypes", "unchecked" })
		final List<CommandInfo<C>> result = (List) list;
		return result;
	}

	@Override
	public <C extends Command> List<CommandInfo<C>>
		getCommandsOfClass(final Class<C> commandClass)
	{
		final ArrayList<CommandInfo<C>> result =
			new ArrayList<CommandInfo<C>>();
		getPluginsOfClass(commandClass.getName(), getCommands(), result);
		return result;
	}

	@Override
	public List<CommandInfo<Command>> getCommandsOfClass(
		final String className)
	{
		final ArrayList<CommandInfo<Command>> result =
			new ArrayList<CommandInfo<Command>>();
		getPluginsOfClass(className, getCommands(), result);
		return result;
	}

	@Override
	public <P extends IPlugin> List<? extends P> createInstancesOfType(
		final Class<P> type)
	{
		return createInstances(getPluginsOfType(type));
	}

	@Override
	public <P extends IPlugin> List<? extends P> createInstances(
		final List<PluginInfo<? extends P>> infos)
	{
		final ArrayList<P> list = new ArrayList<P>();
		for (final PluginInfo<? extends P> info : infos) {
			try {
				final P p = info.createInstance();
				list.add(p);
				// inject ImageJ context, where applicable
				if (p instanceof Contextual) {
					((Contextual) p).setContext(getContext());
				}
				// inject priority, where applicable
				if (p instanceof Prioritized) {
					((Prioritized) p).setPriority(info.getPriority());
				}
			}
			catch (final InstantiableException e) {
				log.error("Cannot create plugin: " + info.getClassName());
			}
		}
		return list;
	}

	@Override
	public Future<Module> run(final String className, final Object... inputs) {
		final CommandInfo<?> command = getCommand(className);
		if (!checkPlugin(command, className)) return null;
		return run(command, inputs);
	}

	@Override
	public Future<Module> run(final String className,
		final Map<String, Object> inputMap)
	{
		final CommandInfo<?> command = getCommand(className);
		if (!checkPlugin(command, className)) return null;
		return run(command, inputMap);
	}

	@Override
	public <C extends Command> Future<CommandModule<C>> run(
		final Class<C> commandClass, final Object... inputs)
	{
		final CommandInfo<C> command = getCommand(commandClass);
		if (!checkPlugin(command, commandClass.getName())) return null;
		@SuppressWarnings({ "rawtypes", "unchecked" })
		final Future<CommandModule<C>> future = (Future) run(command, inputs);
		return future;
	}

	@Override
	public <C extends Command> Future<CommandModule<C>> run(
		final Class<C> commandClass, final Map<String, Object> inputMap)
	{
		final CommandInfo<C> command = getCommand(commandClass);
		if (!checkPlugin(command, commandClass.getName())) return null;
		@SuppressWarnings({ "rawtypes", "unchecked" })
		final Future<CommandModule<C>> future = (Future) run(command, inputMap);
		return future;
	}

	@Override
	public Future<Module> run(final ModuleInfo info, final Object... inputs) {
		return moduleService.run(info, pre(), post(), inputs);
	}

	@Override
	public Future<Module> run(final ModuleInfo info,
		final Map<String, Object> inputMap)
	{
		return moduleService.run(info, pre(), post(), inputMap);
	}

	@Override
	public Future<Module> run(final Module module, final Object... inputs) {
		return moduleService.run(module, pre(), post(), inputs);
	}

	@Override
	public <M extends Module> Future<M> run(final M module,
		final Map<String, Object> inputMap)
	{
		return moduleService.run(module, pre(), post(), inputMap);
	}

	// -- Service methods --

	@Override
	public void initialize() {
		pluginIndex = getContext().getPluginIndex();

		// inform the module service of available commands
		moduleService.addModules(getCommands());
	}

	// -- Helper methods --

	private <T extends PluginInfo<?>> void getPluginsOfClass(
		final String className, final List<? extends PluginInfo<?>> srcList,
		final List<T> destList)
	{
		for (final PluginInfo<?> info : srcList) {
			if (info.getClassName().equals(className)) {
				@SuppressWarnings("unchecked")
				final T match = (T) info;
				destList.add(match);
			}
		}
	}

	/** Gets the first element of the given list, or null if none. */
	private <T> T first(final List<T> list) {
		if (list == null || list.size() == 0) return null;
		return list.get(0);

	}

	private List<? extends PreprocessorPlugin> pre() {
		return createInstancesOfType(PreprocessorPlugin.class);
	}

	private List<? extends PostprocessorPlugin> post() {
		return createInstancesOfType(PostprocessorPlugin.class);
	}

	private boolean checkPlugin(final CommandInfo<?> plugin,
		final String name)
	{
		if (plugin == null) {
			log.error("No such plugin: " + name);
			return false;
		}
		return true;
	}

}
